package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DnaAnalysisMaster extends AbstractBehavior<DnaAnalysisMaster.Command> {

    private List<MasterGuardian.CsvEntry> unanalysedDna;
    private List<AnalysedDnaMessage> analysedDna;
    private ActorRef<Worker.WorkCommand> workers;

    private ActorRef<HashMiningMaster.Command> hashMiningMaster;


    protected interface Command {

    }

    public static final class CsvContent implements Command, akka.actor.NoSerializationVerificationNeeded {
        // List of csv entries is sent from Guardian and received by GeneAnalysisMaster
        private final List<MasterGuardian.CsvEntry> csvEntries;

        public CsvContent(List<MasterGuardian.CsvEntry> csvEntries) {
            this.csvEntries = csvEntries;
        }
    }

    public static final class AnalysedDnaMessage implements Command, RemoteSerializable {
        private final Integer id;
        private final String name;
        private final Integer partner;
        private final String passwordHash;


        public AnalysedDnaMessage(Integer id, String name, Integer partner, String passwordHash) {
            this.id = id;
            this.name = name;
            this.partner = partner;
            this.passwordHash = passwordHash;
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Integer getPartner() {
            return partner;
        }

        public String getPasswordHash() {
            return passwordHash;
        }
    }

    public static Behavior<Command> create(ActorRef<Worker.WorkCommand> workers, ActorRef<HashMiningMaster.Command> hashMiningMaster) {
        return Behaviors.setup(context -> new DnaAnalysisMaster(context, workers, hashMiningMaster));
    }

    private DnaAnalysisMaster(ActorContext<Command> context, ActorRef<Worker.WorkCommand> workers, ActorRef<HashMiningMaster.Command> hashMiningMaster) {
        super(context);
        this.workers = workers;
        this.hashMiningMaster = hashMiningMaster;
    }


    @Override
    //when it receives any command message
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CsvContent.class, this::onCsvHashInput)
                .onMessage(AnalysedDnaMessage.class, this::onAnalysedDnaMessage)
                .build();
    }

    //received by main
    private Behavior<Command> onCsvHashInput(CsvContent command) {
        getContext().getLog().info("Csv received from main!");
        //unanalysedDna is sent to worker one by one that's why we store it here
        unanalysedDna = new ArrayList<>(command.csvEntries);
        analysedDna = new ArrayList<>();
        //send messages (all elements of unanalysedDna to Worker)
        command.csvEntries.forEach(csvEntry -> {
            workers.tell(new Worker.DnaAnalysisMessage(command.csvEntries, csvEntry.id - 1, getContext().getSelf()));
        });
        return this;
    }

    private Behavior<Command> onAnalysedDnaMessage(AnalysedDnaMessage command) {
        getContext().getLog().debug("Received analyzed dna for {} with partner {}!", command.id, command.partner);
        analysedDna.add(command);
        unanalysedDna.removeIf(x -> x.id.equals(command.id));
        if (!unanalysedDna.isEmpty()) {
            return this;
        } else {
            analysedDna.sort(Comparator.comparing(AnalysedDnaMessage::getId));
            hashMiningMaster.tell(new HashMiningMaster.DnaAnalysisMessages(analysedDna));

            return Behaviors.stopped(() -> getContext().getLog().info("all done! GeneAnalysisMaster shutting down"));
        }
    }
}

