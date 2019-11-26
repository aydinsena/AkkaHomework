package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;


//forwards Addworker messages from remote slave to Guardian,
//because for some reason the Guardian can't be targeted remotely in Akka typed
public class AddWorkerProxy extends AbstractBehavior<MasterGuardian.AddWorker> {
  private ActorRef<MasterGuardian.Command> guardian;

  public static Behavior<MasterGuardian.AddWorker> create(ActorRef<MasterGuardian.Command> guardian) {
    return Behaviors.setup(context -> new AddWorkerProxy(context, guardian));
  }

  private AddWorkerProxy(ActorContext<MasterGuardian.AddWorker> context, ActorRef<MasterGuardian.Command> guardian) {
    super(context);
    this.guardian = guardian;
    context.getLog().info("spawned proxy under " + context.getSelf().toString());
  }

  @Override
  public Receive<MasterGuardian.AddWorker> createReceive() {
    return newReceiveBuilder()
            .onMessage(MasterGuardian.AddWorker.class, this::onAddWorker)
            .build();
  }

  private Behavior<MasterGuardian.AddWorker> onAddWorker(MasterGuardian.AddWorker command) {
    guardian.tell(command);
    getContext().getLog().info("forwarded command to guardian!");
    return this;
  }
}


