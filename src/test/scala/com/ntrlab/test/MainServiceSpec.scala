package com.ntrlab.test

import com.ntrlab.test.entity.{LastRunStats, ProcedureResult}
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest

import scala.concurrent.Future

class MainServiceSpec extends Specification with Specs2RouteTest with MainService {
  def actorRefFactory = system

  "MainService" should {

    "don't handle root path" in {
      Get("/") ~> route ~> check {
        handled === false
      }
    }
  }

  val topicList = List("topic1", "topic2", "topic3")

  override def getTopicsWithData: Future[List[ProcedureResult]] = {
    Future.successful(topicList.map(name => ProcedureResult(name, List.empty)))
  }

  override def lastrunStatForTopic(topic: String): Future[Option[LastRunStats]] = {
    Future.successful(
      Some(LastRunStats(123, 5, 257, 180))
    )
  }
}
