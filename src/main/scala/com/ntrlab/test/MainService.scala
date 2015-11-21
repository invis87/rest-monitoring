package com.ntrlab.test

import akka.actor.{Props, Actor}
import akka.util.Timeout
import com.ntrlab.test.actors.DirWorker
import com.ntrlab.test.entity._
import spray.http.MediaTypes._
import spray.routing.HttpService
import scala.concurrent.duration._

import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern._

object MainServiceActor {
  def props(path: String): Props = Props(new MainServiceActor(path))
}

class MainServiceActor(path: String) extends Actor with MainService {



  def actorRefFactory = context

  def receive = runRoute(route)

  val worker = context.actorOf(DirWorker.props(path))

  override def getTopicsWithData: Future[List[ProcedureResult]] = {
    (worker ? DirWorker.GetTopics).mapTo[List[ProcedureResult]]
  }

  override def lastrunStatForTopic(topic: String): Future[Option[LastRunStats]] = {
    (worker ? DirWorker.TopicLastRunStat(topic)).mapTo[Option[LastRunStats]]
  }
}

trait MainService extends HttpService {
  import com.ntrlab.test.JsonProtocol._
  implicit val timeout = Timeout(5.seconds)
  import ExecutionContext.Implicits.global

  def getTopicsWithData: Future[List[ProcedureResult]]
  def lastrunStatForTopic(topic: String): Future[Option[LastRunStats]]

  val route = {
    (get & path("topics")) {
    //todo: why this works?
    onComplete(getTopicsWithData) { complete(_) }
    //todo: and this not?!
//      complete {
//        getTopicsWithData
////        ProcedureResult("test", List.empty)
//      }
    } ~
      (get & path("lastrun")) {
        parameter('topic) { topicName =>
          respondWithMediaType(`application/json`) {
            complete {
              s"""{
                 |"test": "Yes",
                 |"result": "timestamp of last run for $topicName",
                 |"timestamp": 42424242
                 |}
                """.stripMargin
            }
          }
        }
      } ~
      (get & pathPrefix("stats")) {
        pathEndOrSingleSlash {
          parameter('topic, 'timestamp) { (topicName, timestamp) =>
            respondWithMediaType(`application/json`) {
              complete {
                s"""{
                   |"test": "Yes",
                   |"result": "statistic for run $timestamp for topic $topicName",
                   |"partitions": ["partition1", "partition2", "partition3"],
                   |"partition1 messages": 42,
                   |"partition2 messages": 42,
                   |"partition2 messages": 42
                   |}
                """.stripMargin
              }
            }
          }
        } ~
          (get & pathPrefix("lastrun")) {
            parameter('topic) { topicName =>
              complete {
                lastrunStatForTopic(topicName)
              }
//              respondWithMediaType(`application/json`) {
//                complete {
//                  s"""{
//                     |"test": "Yes",
//                     |"result": "statistic for last run for topic $topicName",
//                     |"messages count": 42,
//                     |"max messages count": 4,
//                     |"min messages count": 2,
//                     |"mean messages count": 3
//                     |}
//                """.stripMargin
//                }
//              }
            }
          }
      }
  }
}
