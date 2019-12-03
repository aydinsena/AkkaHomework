package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class HashMiningMaster extends AbstractBehavior<HashMiningMaster.Command> {

    private ActorRef<Worker.WorkCommand> workers;

    List<LinearCombinationMaster.CrackedPasswordWithPrefixMessage> crackedPasswordsWithPrefixes;

    List<DnaAnalysisMaster.AnalysedDnaMessage> analysedDna;

    List<ResultMessage> results = new ArrayList<>();

    public interface Command {

    }

    public static final class CrackedPasswordsWithPrefixMessages implements Command {

        private final List<LinearCombinationMaster.CrackedPasswordWithPrefixMessage> crackedPasswordsWithPrefix;

        public CrackedPasswordsWithPrefixMessages(List<LinearCombinationMaster.CrackedPasswordWithPrefixMessage> crackedPasswordsWithPrefix) {
            this.crackedPasswordsWithPrefix = crackedPasswordsWithPrefix;
        }
    }

    public static final class DnaAnalysisMessages implements Command {

        private final List<DnaAnalysisMaster.AnalysedDnaMessage> analysedDna;

        public DnaAnalysisMessages(List<DnaAnalysisMaster.AnalysedDnaMessage> analysedDna) {
            this.analysedDna = analysedDna;
        }
    }

    public static final class MinedHashMessage implements Command, RemoteSerializable {
        private final Integer id;
        private final String partnerHash;

        public MinedHashMessage(Integer id, String partnerHash) {
            this.id = id;
            this.partnerHash = partnerHash;
        }
    }

    public static final class ResultMessage {
        private final Integer id;
        private final String name;
        private final Integer password;
        private final Integer prefix;
        private final Integer partner;
        private final String hash;

        public ResultMessage(Integer id, String name, Integer password, Integer prefix, Integer partner, String hash) {
            this.id = id;
            this.name = name;
            this.password = password;
            this.prefix = prefix;
            this.partner = partner;
            this.hash = hash;
        }

        public String toString() {
            return id + ";" +
                    name + ";" +
                    password + ";" +
                    prefix + ";" +
                    partner + ";" +
                    hash + ";";
        }

        public Integer getId() {
            return id;
        }
    }

    // when guardian creates LinearCombinationMaster, it gives the reference of workers to the master
    public static Behavior<Command> create(ActorRef<Worker.WorkCommand> workers) {
        return Behaviors.setup(context -> new HashMiningMaster(context, workers));
    }

    public HashMiningMaster(ActorContext<Command> context, ActorRef<Worker.WorkCommand> workers) {
        super(context);
        this.workers = workers;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CrackedPasswordsWithPrefixMessages.class, this::onCrackedPasswordsWithPrefixMessages)
                .onMessage(DnaAnalysisMessages.class, this::onDnaAnalysisMessages)
                .onMessage(MinedHashMessage.class, this::onMinedHashMessage)
                .build();
    }

    private Behavior<Command> onCrackedPasswordsWithPrefixMessages(CrackedPasswordsWithPrefixMessages command) {
        getContext().getLog().info("Received cracked passwords with prefixes from LinearCombinationMaster");
        this.crackedPasswordsWithPrefixes = new ArrayList<>(command.crackedPasswordsWithPrefix);

        //Debug: print everything
        command.crackedPasswordsWithPrefix.forEach(m -> {
            getContext().getLog().info("cracked password with prefix: {}, {}, {}, {}", m.getId(), m.getName(), m.getCrackedPassword(), m.getPrefix());
        });

        if (this.analysedDna == null) {
            //gene analysis not finished yet, wait for geneAnalysisMessage to arrive
            return this;
        } else {
            return processOnceBothMessagesArrived();
        }
    }

    private Behavior<Command> onDnaAnalysisMessages(DnaAnalysisMessages command) {
        getContext().getLog().info("Received cracked passwords with prefixes from LinearCombinationMaster");
        this.analysedDna = new ArrayList<>(command.analysedDna);

        //Debug: print everything
        command.analysedDna.forEach(m -> {
            getContext().getLog().info("cracked dna: {}, {}, {}, {}", m.getId(), m.getName(), m.getPartner(), m.getPasswordHash());
        });
        if (this.crackedPasswordsWithPrefixes == null) {
            //wait for other message to arrive
            return this;
        } else {
            return processOnceBothMessagesArrived();
        }
    }

    private Behavior<Command> processOnceBothMessagesArrived() {
        this.analysedDna.forEach(ad -> {
            LinearCombinationMaster.CrackedPasswordWithPrefixMessage cp =
                    this.crackedPasswordsWithPrefixes.stream().filter(x -> x.getId().equals(ad.getId()))
                            .findFirst().orElseThrow(() -> new RuntimeException("id missing"));
            workers.tell(new Worker.HashMiningMessage(ad.getPartner(), ad.getId(), cp.getPrefix(), getContext().getSelf()));
        });

        return this;
    }

    private Behavior<Command> onMinedHashMessage(MinedHashMessage command) {
        getContext().getLog().debug("Received mined hash for person {} with partner hash {}", command.id, command.partnerHash);

        DnaAnalysisMaster.AnalysedDnaMessage ad = this.analysedDna.stream().filter(x -> x.getId().equals(command.id))
                .findFirst().orElseThrow(() -> new RuntimeException("id missing"));
        LinearCombinationMaster.CrackedPasswordWithPrefixMessage cp = this.crackedPasswordsWithPrefixes.stream().filter(x -> x.getId().equals(command.id))
                .findFirst().orElseThrow(() -> new RuntimeException("id missing"));

        ResultMessage result = new ResultMessage(command.id, ad.getName(), cp.getCrackedPassword(), cp.getPrefix(), ad.getPartner(), command.partnerHash);
        this.results.add(result);
        this.analysedDna.remove(ad);
        this.crackedPasswordsWithPrefixes.remove(cp);

        if (this.analysedDna.isEmpty()) {
            if (!this.crackedPasswordsWithPrefixes.isEmpty()) throw new RuntimeException("result list size mismatch");

            this.results.sort(Comparator.comparing(ResultMessage::getId));
            this.results.forEach(r -> getContext().getLog().info(r.toString()));

            return Behaviors.stopped(() -> getContext().getLog().info("all done! HashMiningMaster shutting down"));
        } else {
            return this;
        }
    }
}
