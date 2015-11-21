package com.ntrlab.test

import java.io.File

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import akka.pattern._
import akka.util.Timeout
import com.ntrlab.test.actors.DirWorker
import spray.can.Http
import scala.concurrent.duration._

object Boot extends App {

  args.length match {
    case 0 =>
      println("Please specify directory to monitoring")
      System.exit(0)
    case 1 =>
      val path = args(0)
      val file = new File(path)
      if(!file.exists || !file.isDirectory){
        println(s"Directory $path doesn't exists!")
        System.exit(0)
      }

      start(path)
    case _ =>
      println("Too many arguments!")
      System.exit(0)
  }

  def start(path: String) = {
    implicit val system = ActorSystem("rest-monitoring")

    val service = system.actorOf(MainServiceActor.props(path), "main-service")

    implicit val timeout = Timeout(5.seconds)

    IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
  }

  def startTest(path: String) = {
    implicit val system = ActorSystem("rest-monitoring")
    implicit val timeout = Timeout(5.seconds)
    import system.dispatcher

    val worker = system.actorOf(DirWorker.props(path))

    val list = worker ? DirWorker.TopicLastRunStat("testTopic")
    list.foreach(println)

    system.terminate()
  }
}
