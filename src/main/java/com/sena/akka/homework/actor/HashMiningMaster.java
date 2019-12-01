package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.sena.akka.homework.utils.AnalyzeUtils;

import java.util.List;
import java.util.stream.Collectors;

public class HashMiningMaster extends AbstractBehavior<HashMiningMaster.Command> {
    private ActorRef<Worker.WorkCommand> workers;
    CrackedPasswordsWithPrefixMessages crackedPasswordsWithPrefixMessages;
    GeneAnalysisMessage geneAnalysisMessage;


    public interface Command {

    }

    public static final class CrackedPasswordsWithPrefixMessages implements Command {
        private final List<LinearCombinationMaster.CrackedPasswordWithPrefixMessage> crackedPasswordsWithPrefix;

        public CrackedPasswordsWithPrefixMessages(List<LinearCombinationMaster.CrackedPasswordWithPrefixMessage> crackedPasswordsWithPrefix) {
            this.crackedPasswordsWithPrefix = crackedPasswordsWithPrefix;
        }
    }

    public static final class GeneAnalysisMessage implements Command {
        //TODO
        //private final List<LinearCombinationMaster.CrackedPasswordWithPrefixMessage> crackedPasswordsWithPrefix;

        public GeneAnalysisMessage() {
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
                .build();
    }

    private Behavior<Command> onCrackedPasswordsWithPrefixMessages(CrackedPasswordsWithPrefixMessages command) {
        getContext().getLog().info("Received cracked passwords with prefixes from LinearCombinationMaster");
        this.crackedPasswordsWithPrefixMessages = command;

        //Debug: print everything
        command.crackedPasswordsWithPrefix.forEach(m -> {
            getContext().getLog().info("cracked password with prefix: {}, {}, {}, {}", m.getId(), m.getName(), m.getCrackedPassword(), m.getPrefix());
        });

        //TODO: remove this
        return Behaviors.stopped(() -> getContext().getLog().info("all done! HashMiningMaster shutting down"));

//        if (this.geneAnalysisMessage == null) {
//            //gene analysis not finished yet, wait for geneAnalysisMessage to arrive
//            return this;
//        } else {
//            //TODO: start processing
//            return this;
//        }
    }
}
