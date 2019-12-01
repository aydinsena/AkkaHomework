package com.sena.akka.homework.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.ArrayList;
import java.util.List;

public class MasterGuardian extends AbstractBehavior<MasterGuardian.Command> {

    protected interface Command {

    }

    public static final ServiceKey<Worker.WorkCommand> serviceKey = ServiceKey.create(Worker.WorkCommand.class, "worker-key");
    private ActorRef<Worker.WorkCommand> workers;
    private ActorRef<PasswordCrackingMaster.Command> passwordCrackingMaster;
    private ActorRef<LinearCombinationMaster.Command> linearCombinationMaster;
    private ActorRef<HashMiningMaster.Command> hashMiningMaster;


    public static final class CsvEntry implements akka.actor.NoSerializationVerificationNeeded {
        public final Integer id;
        public final String name;
        public final String passwordHash;
        public final String gene;

        public CsvEntry(Integer id, String name, String passwordHash, String gene) {
            this.id = id;
            this.name = name;
            this.passwordHash = passwordHash;
            this.gene = gene;
        }
    }

    //receives CSV from AkkaStart
    public static class Start implements Command {//}, akka.actor.NoSerializationVerificationNeeded {
        public final List<MasterGuardian.CsvEntry> csvEntries;
        private final int numWorkers;

        public Start(List<MasterGuardian.CsvEntry> csvEntries, int numWorkers) {
            this.csvEntries = csvEntries;
            this.numWorkers = numWorkers;
        }
    }

    public static class AddWorker implements Command, RemoteSerializable {
        private final ActorRef<SlaveGuardian.Command> slaveGuardian;
        private final ActorRef<Worker.WorkCommand> worker;

        @JsonCreator
        public AddWorker(ActorRef<SlaveGuardian.Command> slaveGuardian, ActorRef<Worker.WorkCommand> worker) {
            this.slaveGuardian = slaveGuardian;
            this.worker = worker;
        }
    }

    private List<ActorRef<SlaveGuardian.Command>> slaveGuardians;

    public static Behavior<Command> create() {
        return Behaviors.setup(MasterGuardian::new);
    }

    private MasterGuardian(ActorContext<Command> context) {
        super(context);
        slaveGuardians = new ArrayList<>();
        ActorRef<MasterGuardian.AddWorker> dummy = context.spawn(AddWorkerProxy.create(context.getSelf()), "add-worker-proxy");
        context.getLog().info("created " + context.getSelf().toString());
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Start.class, this::onStart)
                .onMessage(AddWorker.class, this::onAddWorker)
                .onSignal(Terminated.class, this::onTerminated)
                .build();
    }

    //This is executed when start message is received: passwordCrackingMaster is created in this case
    private Behavior<Command> onStart(Start command) {
        //TODO: read CSV
        //#create-worker pool
        PoolRouter<Worker.WorkCommand> pool =
                Routers.pool(
                        command.numWorkers,
                        Behaviors.supervise(Worker.create()).onFailure(SupervisorStrategy.restart()));
        //this is where worker pool is spawned
        ActorRef<Worker.WorkCommand> localWorkerPool = getContext().spawn(pool, "local-worker-pool");
        getContext().getSystem().receptionist().tell(Receptionist.register(serviceKey, localWorkerPool));
        GroupRouter<Worker.WorkCommand> group = Routers.group(serviceKey);
        workers = getContext().spawn(group, "worker-group");

        //create-HashMiningMaster
        hashMiningMaster =
                getContext().spawn(HashMiningMaster.create(workers), "hashMiningMaster");
        getContext().watch(hashMiningMaster);

        //create-LinearCombinationMaster
        linearCombinationMaster =
                getContext().spawn(LinearCombinationMaster.create(workers, hashMiningMaster), "linearCombinationMaster");

        //#create-PasswordCrackingMaster
        passwordCrackingMaster =
                getContext().spawn(PasswordCrackingMaster.create(workers, linearCombinationMaster), "passwordCrackingMaster");
        //after creating PasswordCrackingMaster, sends the csv input to PasswordCrackingMaster
        passwordCrackingMaster.tell(new PasswordCrackingMaster.CsvHashInput(command.csvEntries));

        return this;
    }

    private Behavior<Command> onAddWorker(AddWorker command) {
        //TODO add worker to pool
        getContext().getLog().info("registering new worker");
        slaveGuardians.add(command.slaveGuardian);
        getContext().getSystem().receptionist().tell(Receptionist.register(serviceKey, command.worker));
        return this;
    }

    private Behavior<Command> onTerminated(Terminated terminated) {
        getContext().getSystem().log().info("Job stopped: {}", terminated.getRef().path().name());
        //TODO find out proper way to shut down once everything is done
        if (terminated.getRef().path().name().equals("hashMiningMaster")) {
            getContext().getLog().info("everything done, guardian shutting down");
            slaveGuardians.forEach(sg -> sg.tell(new SlaveGuardian.Stop()));
            return Behaviors.stopped();
        }
        return this;
    }
}
