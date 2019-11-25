package com.sena.akka.homework.actor;

import akka.actor.typed.ActorSystem;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.sena.akka.homework.utils.AkkaUtils;
import com.typesafe.config.Config;



import java.io.IOException;

public class AkkaStart {

  public static void main(String[] args) {
    JCommander jCommander = JCommander.newBuilder()
            .addCommand("master", new CommandMaster())
            .addCommand("slave", new CommandSlave())
            .build();

    try {
      jCommander.parse(args);

      if (jCommander.getParsedCommand() == null) {
        startMaster();
      }

      // Start a master or slave.
      switch (jCommander.getParsedCommand()) {
        case "master":
          startMaster();
          break;
        case "slave":
          startSlave();
          break;
        default:
          throw new AssertionError();
      }

    } catch (ParameterException e) {
      System.out.printf("Could not parse args: %s\n", e.getMessage());
      if (jCommander.getParsedCommand() == null) {
        jCommander.usage();
      } else {
        jCommander.usage(jCommander.getParsedCommand());
      }
      System.exit(1);
    }


  }

  private static void startMaster() {
    String host = "localhost";
    int port = 4567;

    final Config config = AkkaUtils.createRemoteAkkaConfig(host, port);

    System.out.println("config:");
    System.out.println(config.toString());
    //#actor-system
    final ActorSystem<Guardian.Command> guardian = ActorSystem.create(Guardian.create(), "guardian", config);

    //try adding a remote worker pool
    final ActorSystem<Worker.WorkCommand> remoteWorkers = ActorSystem.create(Worker.create(), "remote-workers", config);
    guardian.tell(new Guardian.AddWorker(remoteWorkers));

    //#main-send-messages
    guardian.tell(new Guardian.Start("start"));
    //#main-send-messages

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {
    } finally {

    }
  }

  private static void startSlave() {

  }

  @Parameters(commandDescription = "start a master actor system")
  static class CommandMaster {

  }

  @Parameters(commandDescription = "start a slave actor system")
  static class CommandSlave {

  }
}
