package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.sena.akka.homework.utils.AnalyzeUtils;


public class Worker extends AbstractBehavior<Worker.WorkCommand> {

  protected interface WorkCommand {

  }

  public static final class HashMessage implements WorkCommand, RemoteSerializable {
    private final Integer id;
    private final String name;
    private final String passwordHash;
    //in order to reply to master
    private final ActorRef<PasswordCrackingMaster.Command> replyTo;

    public HashMessage(Integer id, String name, String passwordHash, ActorRef<PasswordCrackingMaster.Command> replyTo) {
      this.id = id;
      this.name = name;
      this.passwordHash = passwordHash;
      this.replyTo = replyTo;
    }
  }

  public static Behavior<WorkCommand> create() {
    return Behaviors.setup(Worker::new);
  }

  private Worker(ActorContext<WorkCommand> context) {
    super(context);
  }

  @Override
  public Receive<WorkCommand> createReceive() {
    return newReceiveBuilder()
            .onMessage(HashMessage.class, this::onHashMessage)
            .build();
  }

  private Behavior<WorkCommand> onHashMessage(HashMessage command) {
    getContext().getLog().info("Received hash message {}!", command.passwordHash);
    Integer pw = crackPassword(command.passwordHash);
    command.replyTo.tell(new PasswordCrackingMaster.CrackedPasswordMessage(command.id, command.name, pw));
    return this;
  }

  private Integer crackPassword(String hash) {
    try {
      getContext().getLog().info("attempting to crack password for hash " + hash);
      return AnalyzeUtils.unhash(hash);
    } catch (RuntimeException e) {
      getContext().getLog().info("cracking failed for hash  " + hash);
      return 0;
    }
  }
}


