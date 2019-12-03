package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sena.akka.homework.utils.AnalyzeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LinearCombinationMaster extends AbstractBehavior<LinearCombinationMaster.Command> {

    private ActorRef<Worker.WorkCommand> workers;
    private ActorRef<HashMiningMaster.Command> hashMiningMaster;
    CrackedPasswordMessages crackedPasswordMessages;
    int [] passwords;
    private long rangeCounter;
    //TODO tweak params
    private static int rangeIncrease = 10000000;
    private static int numWorkersUsed = 20;

    public interface Command {

    }

    public static final class CrackedPasswordMessages implements Command {
        private final List<PasswordCrackingMaster.CrackedPasswordMessage> crackedPasswords;

        public CrackedPasswordMessages(List<PasswordCrackingMaster.CrackedPasswordMessage> crackedPasswords) {
            this.crackedPasswords = crackedPasswords;
        }

        public int[] getPasswordArray() {
            //get the password fields and transform them to an int array
            List<Integer> passwordList = this.crackedPasswords
                    .stream()
                    .map(PasswordCrackingMaster.CrackedPasswordMessage::getCrackedPassword)
                    .collect(Collectors.toList());
            return passwordList.stream().mapToInt(i -> i).toArray();
        }
    }


    public static final class PrefixesFound implements Command, RemoteSerializable {

        private final int[] prefixes;

        @JsonCreator
        public PrefixesFound(int[] prefixes) {
            this.prefixes = prefixes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static final class PrefixesNotFound implements Command, RemoteSerializable {

        public PrefixesNotFound() {
        }
    }

    public static final class CrackedPasswordWithPrefixMessage implements Command, RemoteSerializable {
        private final Integer id;
        private final String name;
        private final Integer crackedPassword;
        private final Integer prefix;


        public CrackedPasswordWithPrefixMessage(Integer id, String name, Integer crackedPassword, Integer prefix) {
            this.id = id;
            this.name = name;
            this.crackedPassword = crackedPassword;
            this.prefix = prefix;
        }

        public Integer getId() {
            return id;
        }

        public Integer getCrackedPassword() {
            return crackedPassword;
        }

        public String getName() {
            return name;
        }

        public Integer getPrefix() {
            return prefix;
        }
    }

    // when guardian creates LinearCombinationMaster, it gives the reference of workers to the master
    public static Behavior<Command> create(ActorRef<Worker.WorkCommand> workers, ActorRef<HashMiningMaster.Command> hashMiningMaster) {
        return Behaviors.setup(context -> new LinearCombinationMaster(context, workers, hashMiningMaster));
    }

    public LinearCombinationMaster(ActorContext<Command> context, ActorRef<Worker.WorkCommand> workers,  ActorRef<HashMiningMaster.Command> hashMiningMaster) {
        super(context);
        this.workers = workers;
        this.hashMiningMaster = hashMiningMaster;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CrackedPasswordMessages.class, this::onCrackedPasswordMessages)
                .onMessage(PrefixesFound.class, this::onPrefixesFound)
                .onMessage(PrefixesNotFound.class, this::onPrefixesNotFound)
                .build();
    }

    private Behavior<Command> onCrackedPasswordMessages(CrackedPasswordMessages command) {
        getContext().getLog().info("Received cracked passwords from PasswordCrackingMaster");
        this.crackedPasswordMessages = command;
        this.passwords = command.getPasswordArray();
        this.rangeCounter = 0;
        for (int i = 0; i < LinearCombinationMaster.numWorkersUsed; i++) {
            workers.tell(new Worker.LinearCombinationMessage(
                    this.passwords,
                    rangeCounter,
                    rangeCounter + LinearCombinationMaster.rangeIncrease,
                    getContext().getSelf()
            ));
            rangeCounter = rangeCounter + LinearCombinationMaster.rangeIncrease;
        }
        return this;
    }

    private Behavior<Command> onPrefixesNotFound(PrefixesNotFound command) {
        getContext().getLog().debug("Received prefixes not found from the Worker, sending another range starting at " + rangeCounter);
        workers.tell(new Worker.LinearCombinationMessage(
                this.passwords,
                rangeCounter,
                rangeCounter + LinearCombinationMaster.rangeIncrease,
                getContext().getSelf()
        ));
        rangeCounter = rangeCounter + LinearCombinationMaster.rangeIncrease;
        return this;
    }

    private Behavior<Command> onPrefixesFound(PrefixesFound command) {
        int checkSum = AnalyzeUtils.sum(this.passwords, command.prefixes);
        if (checkSum != 0) {
            throw new RuntimeException("prefix checksum not 0, it is: " + checkSum + " for prefixes " + Arrays.toString(command.prefixes));
        }
        getContext().getLog().info("Received the right prefixes from the Worker, sum is " + checkSum + ". prefix is " + Arrays.toString(command.prefixes));
        List<CrackedPasswordWithPrefixMessage> crackedPasswordWithPrefixMessageList =
                this.crackedPasswordMessages.crackedPasswords.stream()
                .map(m -> new CrackedPasswordWithPrefixMessage(
                        m.getId(),
                        m.getName(),
                        m.getCrackedPassword(),
                        command.prefixes[m.getId() - 1]
                        ))
                .collect(Collectors.toList());
        hashMiningMaster.tell(new HashMiningMaster.CrackedPasswordsWithPrefixMessages(crackedPasswordWithPrefixMessageList));

        return Behaviors.stopped(() -> getContext().getLog().info("all done! LinearCombinationMaster shutting down"));
    }
}
