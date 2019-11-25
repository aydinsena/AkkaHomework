package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.List;

// PasswordCrackingMaster receives CsvHashInput message
// PasswordCrackingMaster receives CsvHashInput (from main) and CrackedPasswordMessage (from worker)
//PasswordCrackingMaster.Command shows that PasswordCrackingMaster can receive command message. That's why
//we created an interface called Command so we can put two different type of objects in it.
//the reason we didn't create interface in PasswordCrackingWorker class because worker class receives only one type of object
public class PasswordCrackingMaster extends AbstractBehavior<PasswordCrackingMaster.Command> {

    //uncrackedHashes list is used to send PasswordCrackingWorker
    private List<Guardian.CsvEntry> uncrackedHashes;
    private List<CrackedPasswordMessage> crackedPasswords;
    private ActorRef<Worker.WorkCommand> workers;

    protected interface Command {

    }

    public static final class CsvHashInput implements Command {
        // List of csv entries is sent from Guardian and received by PasswordCrackingMaster
        public final List<Guardian.CsvEntry> csvEntries;

        public CsvHashInput(List<Guardian.CsvEntry> csvEntries) {
            this.csvEntries = csvEntries;
        }
    }

    public static final class CrackedPasswordMessage implements Command {
        public final Integer id;
        public final String name;
        public final Integer crackedPassword;


        public CrackedPasswordMessage(Integer id, String name, Integer crackedPassword) {
            this.id = id;
            this.name = name;
            this.crackedPassword = crackedPassword;
        }

    }

    // when guardian creates PasswordCrakingMaster, it gives the reference of workers to the master
    public static Behavior<Command> create(ActorRef<Worker.WorkCommand> workers) {
        return Behaviors.setup(context -> new PasswordCrackingMaster(context, workers));
    }

    private PasswordCrackingMaster(ActorContext<Command> context, ActorRef<Worker.WorkCommand> workers) {
        super(context);
        this.workers = workers;
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
        uncrackedHashes = command.csvEntries;
        crackedPasswords = new ArrayList<>();
        //store the first element of uncrackedHashes list
        Guardian.CsvEntry csv = uncrackedHashes.get(0);
        //send message (that is the first element of uncrackedHashes to Worker)
        workers.tell(new Worker.HashMessage(csv.id, csv.name, csv.passwordHash, getContext().getSelf()));
        return this;
    }

    //received by master (after when cracked password is received by master)
    private Behavior<Command> onCrackedPasswordMessage(CrackedPasswordMessage command) {
        getContext().getLog().info("Received cracked passwords!");
        // save cracked password
        crackedPasswords.add(command);
        //remove entry of hash that has been cracked
        uncrackedHashes.removeIf(x -> x.id.equals(command.id));
        if (!uncrackedHashes.isEmpty()) {
            Guardian.CsvEntry csv = uncrackedHashes.get(0);
            workers.tell(new Worker.HashMessage(csv.id, csv.name, csv.passwordHash, getContext().getSelf()));
            return this;
        } else {
            //TODO: terminate
            return this;
        }
    }
}

