package com.sena.akka.homework.actor;

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

  private List<PasswordCrackingWorker.HashMessage> uncrackedHashes;
  private List<CrackedPasswordMessage> crackedPasswords;

  protected interface Command {

  }
  public static final class CsvHashInput implements Command {
    public final List<PasswordCrackingWorker.HashMessage> hashMessages;

    public CsvHashInput(List<PasswordCrackingWorker.HashMessage> hashMessages) {
      this.hashMessages = hashMessages;
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
  public static Behavior<Command> create() {
    return Behaviors.setup(PasswordCrackingMaster::new);
  }

  private PasswordCrackingMaster(ActorContext<Command> context) {
    super(context);
     //TODO: create workers
  }


  @Override
  //when it receives any command message
  public Receive<Command> createReceive() {
    return newReceiveBuilder()
            .onMessage(CsvHashInput.class, this::onCsvHashInput)
            .onMessage(CrackedPasswordMessage.class, this::onCrackedPasswordMessage)
            .build();
  }

  private Behavior<Command> onCsvHashInput(CsvHashInput command) {
    getContext().getLog().info("Csv received from main!");
    uncrackedHashes = command.hashMessages;
    crackedPasswords = new ArrayList<>();
    // TODO: send messages to all workers
    return this;
  }

  private Behavior<Command> onCrackedPasswordMessage(CrackedPasswordMessage command) {
    getContext().getLog().info("Received cracked passwords!");
    // TODO: save cracked password
    return this;
  }
}

