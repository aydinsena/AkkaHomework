package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class PasswordCrackingWorker extends AbstractBehavior<PasswordCrackingWorker.HashMessage> {

  public static final class HashMessage {
    public final Integer id;
    public final String name;
    public final String passwordHash;

    public HashMessage(Integer id, String name, String passwordHash) {
      this.id = id;
      this.name = name;
      this.passwordHash = passwordHash;
    }
  }


  public static Behavior<HashMessage> create(ActorRef<PasswordCrackingMaster.CrackedPasswordMessage> master) {
    return Behaviors.setup(context -> new PasswordCrackingWorker(context, master));
  }

  private final ActorRef<PasswordCrackingMaster.CrackedPasswordMessage> master;

  private PasswordCrackingWorker(ActorContext<HashMessage> context, ActorRef<PasswordCrackingMaster.CrackedPasswordMessage> master) {
    super(context);
    this.master = master;
  }

  @Override
  public Receive<HashMessage> createReceive() {
    return newReceiveBuilder()
            .onMessage(HashMessage.class, this::onHashMessage)
            .build();
  }

  private Behavior<HashMessage> onHashMessage(HashMessage command) {
    getContext().getLog().info("Received hash message!");
    Integer pw = crackPassword(command.passwordHash);
    master.tell(new PasswordCrackingMaster.CrackedPasswordMessage(command.id, command.name, pw));
    return this;
  }

  private Integer crackPassword(String hash) {
    //TODO: implement cracking
    return 1111111;
  }
}


