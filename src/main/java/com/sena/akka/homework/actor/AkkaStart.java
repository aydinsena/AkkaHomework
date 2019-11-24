package com.sena.akka.homework.actor;

import akka.actor.typed.ActorSystem;

import java.io.IOException;

public class AkkaStart {
  public static void main(String[] args) {
    //#actor-system
    final ActorSystem<Guardian.Start> guardian = ActorSystem.create(Guardian.create(), "guardian");

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
}
