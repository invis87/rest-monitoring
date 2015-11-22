package com.ntrlab.test

import com.ntrlab.test.entity._
import spray.json._

  object JsonProtocol extends DefaultJsonProtocol {
  implicit val topicsWithDataFormat = jsonFormat1(TopicsWithData)
  implicit val topicLastRunFormat = jsonFormat1(TopicLastRun)
  implicit val procedureStatsFormat = jsonFormat2(ProcedureStats)
  implicit val lastrunStatsFormat = jsonFormat4(LastRunStats)
}