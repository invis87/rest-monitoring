package com.ntrlab.test.entity

case class ProcedureResult(name: String, stats: List[Partition])
case class Partition(number: Int, messages: Long)
case class LastRunStats(msgCount: Long, minMsgInPartition: Long, maxMsgInPartition: Long, meanMsgCount: Double)
