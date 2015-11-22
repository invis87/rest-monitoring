package com.ntrlab.test.entity

case class TopicsWithData(topics: List[String])
case class LastRunStats(msgCount: Long, minMsgInPartition: Long, maxMsgInPartition: Long, meanMsgCount: Double)
case class ProcedureStats(partition: List[Int], partitionMessages: Map[String, Long])
case class TopicLastRun(lastrun: String)
