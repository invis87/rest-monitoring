package com.ntrlab.test

import com.ntrlab.test.entity.{TopicsWithData, TopicLastRun, ProcedureStats, LastRunStats}
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.{ExecutionContextExecutor, Future}

class MainServiceSpec extends Specification with Specs2RouteTest with MainService {

  override def executionContext: ExecutionContextExecutor = system.dispatcher

  def actorRefFactory = system

  import com.ntrlab.test.JsonProtocol._

  val topicList = List("topic1", "topic2", "topic3")
  val procedureResult = Map("1" -> 2245l, "2" -> 223523l, "3" -> 2423l)
  val topicLastRun = ("topic1", "1998-02-02-02-02-02")
  val topicLastRunStats = LastRunStats(123, 5, 257, 180)
  val procedureStats = ProcedureStats(procedureResult.keys.map(_.toInt).toList, procedureResult)

  "MainService" should {

    "don't handle root path" in {
      Get("/") ~> route ~> check {
        handled === false
      }
    }

    "should return all topics" in {
      Get("/topics") ~> route ~> check {
        handled === true
        response.status should be equalTo OK
        responseAs[TopicsWithData].topics should be equalTo topicList
      }
    }

    s"should return lastrun for topic '${topicLastRun._1}'" in {
      Get(s"/lastrun?topic=${topicLastRun._1}") ~> route ~> check {
        handled === true
        response.status should be equalTo OK
        responseAs[TopicLastRun].lastrun === topicLastRun._2
      }
    }

    s"should not return lastrun for topic 'SomeTopic'" in {
      Get("/lastrun?topic=SomeTopic") ~> route ~> check {
        handled === true
        response.status should be equalTo OK
        responseAs[String] === "Cant find info for topic SomeTopic"
      }
    }

    "should return lastrun stats for topic" in {
      Get(s"/stats/lastrun?topic=anytopic") ~> route ~> check {
        handled === true
        response.status should be equalTo OK
        responseAs[LastRunStats] should be equalTo topicLastRunStats
      }
    }

    "should return stats for topic" in {
      Get(s"/stats?topic=anytopic&timestamp=anytimestamp") ~> route ~> check {
        handled === true
        response.status should be equalTo OK
        responseAs[ProcedureStats] should be equalTo procedureStats
      }
    }

  }

  override def getTopicsWithData: Future[Either[String, TopicsWithData]] = {
    Future.successful(Right(TopicsWithData(topicList)))
  }

  override def getTopicStats(topic: String, timestamp: String): Future[Either[String, ProcedureStats]] = {
    Future.successful(Right(procedureStats))
  }

  override def getLastrunStatForTopic(topic: String): Future[Either[String, LastRunStats]] = {
    Future.successful(
      Right(topicLastRunStats)
    )
  }

  override def getTopicLastrun(topic: String): Future[Either[String, TopicLastRun]] = {
    if (topicLastRun._1 == topic) {
      Future.successful(
        Right(TopicLastRun(topicLastRun._2))
      )
    } else {
      Future.successful(
        Left(s"Cant find info for topic $topic")
      )
    }
  }
}
