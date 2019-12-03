package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import scala.Int;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// PasswordCrackingMaster receives CsvHashInput message
// PasswordCrackingMaster receives CsvHashInput (from main) and CrackedPasswordMessage (from worker)
//PasswordCrackingMaster.Command shows that PasswordCrackingMaster can receive command message. That's why
//we created an interface called Command so we can put two different type of objects in it.
//the reason we didn't create interface in PasswordCrackingWorker class because worker class receives only one type of object
public class PasswordCrackingMaster extends AbstractBehavior<PasswordCrackingMaster.Command> {

    //uncrackedHashes list is used to send PasswordCrackingWorker
    private List<MasterGuardian.CsvEntry> uncrackedHashes;
    private List<CrackedPasswordMessage> crackedPasswords;
    private ActorRef<Worker.WorkCommand> workers;

    private ActorRef<LinearCombinationMaster.Command> linearCombinationMaster;


    protected interface Command {

    }

    public static final class CsvHashInput implements Command, akka.actor.NoSerializationVerificationNeeded {
        // List of csv entries is sent from Guardian and received by PasswordCrackingMaster
        private final List<MasterGuardian.CsvEntry> csvEntries;

        public CsvHashInput(List<MasterGuardian.CsvEntry> csvEntries) {
            this.csvEntries = csvEntries;
        }
    }

    public static final class CrackedPasswordMessage implements Command, RemoteSerializable {
        private final Integer id;
        private final String name;
        private final Integer crackedPassword;


        public CrackedPasswordMessage(Integer id, String name, Integer crackedPassword) {
            this.id = id;
            this.name = name;
            this.crackedPassword = crackedPassword;
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getCrackedPassword() {
            return crackedPassword;
        }
    }

    // when guardian creates PasswordCrakingMaster, it gives the reference of workers to the master
    public static Behavior<Command> create(ActorRef<Worker.WorkCommand> workers, ActorRef<LinearCombinationMaster.Command> linearCombinationMaster) {
        return Behaviors.setup(context -> new PasswordCrackingMaster(context, workers, linearCombinationMaster));
    }

    private PasswordCrackingMaster(ActorContext<Command> context, ActorRef<Worker.WorkCommand> workers, ActorRef<LinearCombinationMaster.Command> linearCombinationMaster) {
        super(context);
        this.workers = workers;
        this.linearCombinationMaster = linearCombinationMaster;
    }


    @Override
    //when it receives any command message
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CsvHashInput.class, this::onCsvHashInput)
                .onMessage(CrackedPasswordMessage.class, this::onCrackedPasswordMessage)
                .build();
    }

    //received by main
    private Behavior<Command> onCsvHashInput(CsvHashInput command) {
        getContext().getLog().info("Csv received from main!");
        //uncrackedHashes is sent to worker one by one that's why we store it here
        uncrackedHashes = new ArrayList<>(command.csvEntries);
        crackedPasswords = new ArrayList<>();
        //send all messages
        command.csvEntries.forEach(csv -> {
            workers.tell(new Worker.HashMessage(csv.id, csv.name, csv.passwordHash, getContext().getSelf()));
        });
        return this;
    }

    //received by master (after when cracked password is received by master)
    private Behavior<Command> onCrackedPasswordMessage(CrackedPasswordMessage command) {
        getContext().getLog().debug("Received cracked password {} for {}!", command.crackedPassword, command.id);
        // save cracked password
        crackedPasswords.add(command);
        //remove entry of hash that has been cracked
        uncrackedHashes.removeIf(x -> x.id.equals(command.id));
        if (!uncrackedHashes.isEmpty()) {
            return this;
        } else {
            crackedPasswords.sort(Comparator.comparing(CrackedPasswordMessage::getId));
            linearCombinationMaster.tell(new LinearCombinationMaster.CrackedPasswordMessages(crackedPasswords));

            return Behaviors.stopped(() -> getContext().getLog().info("all done! PasswordCrackingMaster shutting down"));
        }
    }
}

