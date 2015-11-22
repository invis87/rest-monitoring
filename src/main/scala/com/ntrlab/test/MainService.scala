package com.ntrlab.test

import akka.actor.{Props, Actor}
import akka.util.Timeout
import com.ntrlab.test.actors.DirWorker
import com.ntrlab.test.entity._
import spray.routing.HttpService
import scala.concurrent.duration._

import spray.httpx.SprayJsonSupport._

import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.pattern._

object MainServiceActor {
  def props(path: String): Props = Props(new MainServiceActor(path))
}

class MainServiceActor(path: String) extends Actor with MainService {

  override def executionContext: ExecutionContextExecutor = context.dispatcher
  def actorRefFactory = context

  def receive = runRoute(route)

  val worker = context.actorOf(DirWorker.props(path))

  override def getTopicsWithData: Future[Either[String, TopicsWithData]] = {
    (worker ? DirWorker.GetTopicsWithData).mapTo[Either[String, TopicsWithData]]
  }

  def getTopicLastrun(topic: String): Future[Either[String, TopicLastRun]] = {
    (worker ? DirWorker.GetTopicLastRun(topic)).mapTo[Either[String, TopicLastRun]]
  }

  override def getTopicStats(topic: String, timestamp: String): Future[Either[String, ProcedureStats]] = {
    (worker ? DirWorker.GetTopicStats(topic, timestamp)).mapTo[Either[String, ProcedureStats]]
  }

  override def getLastrunStatForTopic(topic: String): Future[Either[String, LastRunStats]]= {
    (worker ? DirWorker.GetTopicLastRunStats(topic)).mapTo[Either[String, LastRunStats]]
  }
}

trait MainService extends HttpService {
  import com.ntrlab.test.JsonProtocol._
  implicit val timeout = Timeout(5.seconds)
  def executionContext: ExecutionContextExecutor
  implicit val execContext = executionContext

  def getTopicsWithData: Future[Either[String, TopicsWithData]]
  def getTopicLastrun(topic: String): Future[Either[String, TopicLastRun]]
  def getTopicStats(topic: String, timestamp: String): Future[Either[String, ProcedureStats]]
  def getLastrunStatForTopic(topic: String): Future[Either[String, LastRunStats]]

  val route = {
    (get & path("topics")) {
      complete {
        getTopicsWithData
      }
    } ~
      (get & path("lastrun")) {
        parameter('topic) { topicName =>
          complete {
            getTopicLastrun(topicName)
          }
        }
      } ~
      (get & pathPrefix("stats")) {
        pathEnd {
          parameter('topic, 'timestamp) { (topicName, timestamp) =>
            complete {
              getTopicStats(topicName, timestamp)
            }
          }
        } ~
          (get & pathPrefix("lastrun")) {
            parameter('topic) { topicName =>
              complete {
                getLastrunStatForTopic(topicName)
              }
            }
          }
      }
  }
}
