package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.sena.akka.homework.utils.AnalyzeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

  public static final class LinearCombinationMessage implements WorkCommand, RemoteSerializable {
    private final int [] passwords;
    private long rangeFrom;
    private long rangeTo;
    private final ActorRef<LinearCombinationMaster.Command> replyTo;

    public LinearCombinationMessage(int[] passwords, long rangeFrom, long rangeTo, ActorRef<LinearCombinationMaster.Command> replyTo) {
      this.passwords = passwords;
      this.rangeFrom = rangeFrom;
      this.rangeTo = rangeTo;
      this.replyTo = replyTo;
    }
  }

  public static final class DnaAnalysisMessage implements WorkCommand, RemoteSerializable {
    List<MasterGuardian.CsvEntry> csvEntries;
    Integer thisPersonIndex;
    private final ActorRef<DnaAnalysisMaster.Command> replyTo;

    public DnaAnalysisMessage(List<MasterGuardian.CsvEntry> csvEntries, Integer thisPersonIndex, ActorRef<DnaAnalysisMaster.Command> replyTo) {
      this.csvEntries = csvEntries;
      this.thisPersonIndex = thisPersonIndex;
      this.replyTo = replyTo;
    }
  }

  public static final class HashMiningMessage implements WorkCommand, RemoteSerializable {
    Integer partnerId;
    Integer id;
    Integer prefix;
    private final ActorRef<HashMiningMaster.Command> replyTo;

    public HashMiningMessage(Integer partnerId, Integer id, Integer prefix, ActorRef<HashMiningMaster.Command> replyTo) {
      this.partnerId = partnerId;
      this.id = id;
      this.prefix = prefix;
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
            .onMessage(LinearCombinationMessage.class, this::onLinearCombinationMessage)
            .onMessage(DnaAnalysisMessage.class, this::onDnaAnalysisMessage)
            .onMessage(HashMiningMessage.class, this::onHashMiningMessage)
            .build();
  }

  private Behavior<WorkCommand> onLinearCombinationMessage(LinearCombinationMessage command) {
    getContext().getLog().debug("Received LinearCombinationMessage {} to {}!", command.rangeFrom, command.rangeTo);

    long t = System.currentTimeMillis();
    int [] prefixes = AnalyzeUtils.solve(command.passwords, command.rangeFrom, command.rangeTo);
    getContext().getLog().debug("solving took: " + (System.currentTimeMillis() - t));
    if (prefixes.length > 0) {
      getContext().getLog().info("found prefixes: " + Arrays.toString(prefixes));
      command.replyTo.tell(new LinearCombinationMaster.PrefixesFound(prefixes));
    } else {
      command.replyTo.tell(new LinearCombinationMaster.PrefixesNotFound());
    }
    return this;
  }

  private Behavior<WorkCommand> onHashMessage(HashMessage command) {
    getContext().getLog().debug("Received hash message {}!", command.passwordHash);
    int pw;
    try {
      getContext().getLog().debug("attempting to crack password for hash " + command.passwordHash);
      pw = AnalyzeUtils.unhash(command.passwordHash);
    } catch (RuntimeException e) {
      getContext().getLog().debug("cracking failed for hash  " + command.passwordHash);
      pw = 0;
    }
    command.replyTo.tell(new PasswordCrackingMaster.CrackedPasswordMessage(command.id, command.name, pw));
    return this;
  }

  private Behavior<WorkCommand> onDnaAnalysisMessage(DnaAnalysisMessage command) {
    getContext().getLog().debug("Received DnaAnalysisMessage {}!", command.thisPersonIndex);
    MasterGuardian.CsvEntry thisPerson = command.csvEntries.get(command.thisPersonIndex);

    Integer partner = AnalyzeUtils.longestOverlapPartner(
          command.thisPersonIndex,
          command.csvEntries.stream()
          .map(e -> e.gene)
          .collect(Collectors.toList())
    ) + 1;

    command.replyTo.tell(new DnaAnalysisMaster.AnalysedDnaMessage(
          thisPerson.id,
          thisPerson.name,
          partner,
          thisPerson.passwordHash
          ));
    return this;
  }

  private Behavior<WorkCommand> onHashMiningMessage(HashMiningMessage command) {
    getContext().getLog().debug("Received HashMiningMessage {}!", command.id);
    String prefix = command.prefix > 0 ? "1" : "0";
    String hash = AnalyzeUtils.findHash(command.partnerId, prefix, 5);

    command.replyTo.tell(new HashMiningMaster.MinedHashMessage(
          command.id,
          hash
    ));
    return this;
  }
}


