package com.ntrlab.test

import com.ntrlab.test.entity._
import spray.json.DefaultJsonProtocol

object JsonProtocol extends DefaultJsonProtocol {
  implicit val partitionFormat = jsonFormat2(Partition)
  implicit val topicFormat = jsonFormat2(ProcedureResult)
  implicit val lastrunStatsFormat = jsonFormat4(LastRunStats)
}