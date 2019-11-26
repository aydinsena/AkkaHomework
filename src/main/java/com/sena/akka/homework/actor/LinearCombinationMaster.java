package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import scala.Int;

import java.util.List;

public class LinearCombinationMaster extends AbstractBehavior<LinearCombinationMaster.Command> {

    private ActorRef<Worker.WorkCommand> workers;

    public interface Command {

    }

    public static final class CrackedPasswordMessages implements Command {
        private final List<PasswordCrackingMaster.CrackedPasswordMessage> crackedPasswords;

        public CrackedPasswordMessages(List<PasswordCrackingMaster.CrackedPasswordMessage> crackedPasswords) {
            this.crackedPasswords = crackedPasswords;
        }
    }
    public static final class Prefixes implements Command {

        private final List<Integer> prefixes ;

        public Prefixes(List<Integer> prefixes) {
            this.prefixes = prefixes;
        }
    }

    // when guardian creates LinearCombinationMaster, it gives the reference of workers to the master
    public static Behavior<Command> create(ActorRef<Worker.WorkCommand> workers) {
        return Behaviors.setup(context -> new LinearCombinationMaster(context, workers));
    }

    public LinearCombinationMaster(ActorContext<Command> context, ActorRef<Worker.WorkCommand> workers) {
        super(context);
        this.workers = workers;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(CrackedPasswordMessages.class, this::onCrackedPasswordMessages)
                .build();
    }

    private Behavior<Command> onCrackedPasswordMessages(CrackedPasswordMessages command) {
        getContext().getLog().info("Received cracked passwords from PasswordCrackingMaster");
        return this;
    }
}
