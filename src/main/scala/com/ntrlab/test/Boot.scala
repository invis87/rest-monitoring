package com.ntrlab.test

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.pattern._
import akka.util.Timeout
import spray.can.Http
import scala.concurrent.duration._

object Boot extends App {

  implicit val system = ActorSystem("rest-monitoring")

  val service = system.actorOf(Props[MainServiceActor], "main-service")

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}
