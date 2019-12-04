package com.sena.akka.homework;

import akka.actor.typed.ActorSystem;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.sena.akka.homework.actor.MasterGuardian;
import com.sena.akka.homework.actor.SlaveGuardian;
import com.sena.akka.homework.utils.AkkaUtils;
import com.sena.akka.homework.utils.CsvUtils;
import com.typesafe.config.Config;



import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class AkkaStart {

  public static void main(String[] args) {
    MasterCommand masterCommand = new MasterCommand();
    SlaveCommand slaveCommand = new SlaveCommand();

    JCommander jCommander = JCommander.newBuilder()
            .addCommand("master", masterCommand)
            .addCommand("slave", slaveCommand)
            .build();

    try {
      jCommander.parse(args);

      if (jCommander.getParsedCommand() == null) {
        startMaster(masterCommand);
      }

      // Start a master or slave.
      switch (jCommander.getParsedCommand()) {
        case "master":
          startMaster(masterCommand);
          break;
        case "slave":
          startSlave(slaveCommand);
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

  //when Master system starts, read in csv and create master guardian
  private static void startMaster(MasterCommand masterCommand) {
    final List<MasterGuardian.CsvEntry> csv = CsvUtils.readCsvAsCsvEntries(masterCommand.fileName);

    String host = getDefaultHost();
    //TODO: find free port when running multiple slaves
    int port = 4567;

    final Config config = AkkaUtils.createRemoteAkkaConfig(host, port);

    //create guardian
    final ActorSystem<MasterGuardian.Command> guardian = ActorSystem.create(MasterGuardian.create(), "guardian", config);

    //tell guardian to start processing
    guardian.tell(new MasterGuardian.Start(csv, masterCommand.numWorkers, masterCommand.numSlaves));

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {
    } finally {
        guardian.terminate();
    }
  }

//when slave is created--> create SlaveGuardian and tell slaveGuardian proxyPath

  private static void startSlave(SlaveCommand slaveCommand) {
    String host = getDefaultHost();
    //finds free port
    int port = 0;

    final Config config = AkkaUtils.createRemoteAkkaConfig(host, port);

    final ActorSystem<SlaveGuardian.Command> slaveGuardian = ActorSystem.create(SlaveGuardian.create(), "remote-worker", config);

    slaveGuardian.tell(new SlaveGuardian.Start("akka://guardian@" + slaveCommand.host + ":4567/user/add-worker-proxy", slaveCommand.numWorkers));

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {
    } finally {
      slaveGuardian.terminate();
    }

  }

  @Parameters(commandDescription = "start a master actor system")
  static class MasterCommand {
    @Parameter(names = {"-w", "--workers"}, description = "number of local workers")
    int numWorkers = 1;

    @Parameter(names = {"-s", "--slaves"}, description = "number of slave systems required to join until processing starts")
    int numSlaves = 2;

    @Parameter(names = {"-i", "--input"}, description = "name of the file that contains data to be processed")
    String fileName = "students.csv";
  }

  @Parameters(commandDescription = "start a slave actor system")
  static class SlaveCommand {

    @Parameter(names = {"-w", "--workers"}, description = "number of workers running on slave system")
    int numWorkers = 4;

    @Parameter(names = {"-h", "--host"}, description = "IP of the host system")
    String host = "localhost";
  }

  private static String getDefaultHost() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "localhost";
    }
  }
}
