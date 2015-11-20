package com.ntrlab.test

import akka.actor.Actor
import spray.http.MediaTypes._
import spray.routing.HttpService

class MainServiceActor extends Actor with MainService {

  def actorRefFactory = context

  def receive = runRoute(testRoute)
}

trait MainService extends HttpService {

  val testRoute =
    path("") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            """{
              |"test": "Ok"
              |}
            """.stripMargin
          }
        }
      }
    }
}
