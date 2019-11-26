package com.sena.akka.homework.actor;

import akka.actor.*;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.javadsl.ActorContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class SlaveGuardian extends AbstractBehavior<SlaveGuardian.Command> implements akka.actor.TypedActor.Receiver  {

    @Override
    public void onReceive(Object message, akka.actor.ActorRef sender) {
        getContext().getLog().info("got a message");
        if (message instanceof ActorIdentity) getContext().getLog().info("got identity");
        else getContext().getLog().info("got message of type" + message.getClass().getName());
    }

    protected interface Command {

    }

    //public static final ServiceKey<Worker.WorkCommand> serviceKey = ServiceKey.create(Worker.WorkCommand.class, "worker-key");
    private ActorRef<Worker.WorkCommand> workers;
    private ActorRef<MasterGuardian.Command> masterGuardian;

    public static class Start implements Command, akka.actor.NoSerializationVerificationNeeded {
        private final String masterGuardianPath;
        private final int numWorkers;

        public Start(String masterGuardianPath, int numWorkers) {
            this.masterGuardianPath = masterGuardianPath;
            this.numWorkers = numWorkers;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Stop implements Command, RemoteSerializable {
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(SlaveGuardian::new);
    }
    private SlaveGuardian(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Start.class, this::onStart)
                .onMessage(Stop.class, this::onStop)
                .build();
    }

    //This is executed when start message is received: passwordCrackingMaster is created in this case
    private Behavior<Command> onStart(Start command) {

        //worker pool creation
        int poolSize = command.numWorkers;
        PoolRouter<Worker.WorkCommand> pool =
                Routers.pool(
                        poolSize,
                        Behaviors.supervise(Worker.create()).onFailure(SupervisorStrategy.restart()));
        //this is where worker pool is spawned
        ActorRef<Worker.WorkCommand> slaveWorkerPool = getContext().spawn(pool, "slave-worker-pool");

        //#find Master Guardian and send addworker message
        ActorSelection masterGuardianSelection = getContext().classicActorContext().actorSelection(command.masterGuardianPath);

        try {
            akka.actor.ActorRef ar = masterGuardianSelection.resolveOne(Duration.ofSeconds(30)).toCompletableFuture().get();
            getContext().getLog().info("got actor " + ar.path());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        masterGuardianSelection.tell(new MasterGuardian.AddWorker(getContext().getSelf(), slaveWorkerPool), Adapter.toClassic(getContext().getSelf()));
        //masterGuardianSelection.tell(new Dummy.DummyCommand("something"), Adapter.toClassic(getContext().getSelf()));
        getContext().getLog().info("sent add worker message to " + command.masterGuardianPath);

        return this;
    }
    private Behavior<Command> onStop(Stop command) {
        getContext().getLog().info("everything done, guardian shutting down");
        return Behaviors.stopped();
    }
}
