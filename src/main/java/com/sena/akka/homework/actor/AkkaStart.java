package com.sena.akka.homework.actor;

import akka.actor.typed.ActorSystem;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.sena.akka.homework.utils.AkkaUtils;
import com.sena.akka.homework.utils.CsvUtils;
import com.typesafe.config.Config;



import java.io.IOException;
import java.util.List;

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
    final List<MasterGuardian.CsvEntry> csv = CsvUtils.readCsvAsCsvEntries("students.csv");
    //csv.forEach(x -> System.out.println(x.id + " " + x.name + " " + x.passwordHash + " " + x.gene));

    String host = "localhost";
    int port = 4567;

    final Config config = AkkaUtils.createRemoteAkkaConfig(host, port);

    //create guardian
    final ActorSystem<MasterGuardian.Command> guardian = ActorSystem.create(MasterGuardian.create(), "guardian", config);

    //sleep a little to give time for slave systems to connect. TODO: wait until slave systems are connected
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    //tell guardian to start processing
    guardian.tell(new MasterGuardian.Start(csv, 4));

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {
    } finally {
        guardian.terminate();
    }
  }

  private static void startSlave() {
    String host = "localhost";
    int port = 7654;

    final Config config = AkkaUtils.createRemoteAkkaConfig(host, port);

    final ActorSystem<SlaveGuardian.Command> slaveGuardian = ActorSystem.create(SlaveGuardian.create(), "remote-worker", config);

    slaveGuardian.tell(new SlaveGuardian.Start("akka://guardian@localhost:4567/user/add-worker-proxy", 4));

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {
    } finally {
      slaveGuardian.terminate();
    }

  }

  @Parameters(commandDescription = "start a master actor system")
  static class CommandMaster {

  }

  @Parameters(commandDescription = "start a slave actor system")
  static class CommandSlave {

  }
}
