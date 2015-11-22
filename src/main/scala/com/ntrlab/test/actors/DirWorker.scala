package com.ntrlab.test.actors

import java.io.{FilenameFilter, File}
import java.text.SimpleDateFormat

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor}
import com.ntrlab.test.actors.DirWorker._
import com.ntrlab.test.entity._

import scala.util.{Failure, Success, Try}

object DirWorker {
  def props(path: String) = Props(new DirWorker(path))

  case object GetTopicsWithData

  case class GetTopicLastRunStats(topic: String)
  case class GetTopicStats(topic: String, timestamp: String)
  case class GetTopicLastRun(topic: String)
}

class DirWorker(path: String) extends Actor {

  case class ProcedureResult(topic: String, stats: List[Partition])
  case class Partition(number: Int, messages: Long)

  override def receive: Receive = {
    case GetTopicsWithData => sender ! getTopicsWithData
    case GetTopicLastRun(topic) => sender ! getTopicLastRun(topic)
    case GetTopicLastRunStats(topic) => sender ! getLastOffsetResult(topic).right.map(lastRunStats)
    case GetTopicStats(topic, timestamp) => sender ! getOffsetFile(topic, timestamp).right.map(topicStats)
  }

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss")

  private val offset = "offset.csv"
  private def topicHistoryDir(topic: String) = new File(path, s"$topic/history")
  private def offsetFile(topic: String, timestamp: String) = new File(path, s"$topic/history/$timestamp/$offset")

  private val emptyTopicsFilter = new FilenameFilter() {
    def accept(dir: File, name: String): Boolean = {
       val history = new File(dir, s"$name/history")
      if(history.exists() && history.isDirectory) {
        history.listFiles().nonEmpty
      } else false
    }
  }

  private def getTopicsWithData = {
    val topicsTry = Try {
      val rootDir = new File(path)
      rootDir.listFiles(emptyTopicsFilter).map(_.getName)
    }

    topicsTry match {
      case Failure(e) => Left("Can't get list of topics.")
      case Success(names) => Right(TopicsWithData(names.toList))
    }
  }

  private def getTopicLastRun(topic: String) = {
    val lastTimestamp = getLastOffset(topic)

    lastTimestamp match {
      case Failure(e) => Left(s"Can't get last timestamp for '$topic'")
      case Success(timeStmp) => Right(TopicLastRun(timeStmp))
    }
  }

  private def getLastOffsetResult(topic: String): Either[String,ProcedureResult] = {
    val lastTimestamp = getLastOffset(topic)

    lastTimestamp match {
      case Failure(e) => Left(s"Can't get last offset for topic '$topic'")
      case Success(timeStmp) => getOffsetFile(topic, timeStmp)
    }
  }

  private def getOffsetFile(topic: String, timestamp: String): Either[String,ProcedureResult] = {
    val file = offsetFile(topic, timestamp)
    if(file.exists &&  file.isFile) Right(readProcedureResult(topic, file))
    else Left(s"There is no offset file for timestamp $timestamp")
  }

  private def lastRunStats(procRes: ProcedureResult): LastRunStats = {

    val messages = procRes.stats.map(_.messages)
    val sum = messages.sum
    LastRunStats(
      sum,
      messages.min,
      messages.max,
      sum / messages.length
    )
  }

  private def topicStats(procRes: ProcedureResult): ProcedureStats = {
    val map = procRes.stats.map(part => part.number.toString -> part.messages)(collection.breakOut): Map[String, Long]
    ProcedureStats(map.keys.map(_.toInt).toList, map)
  }

  private def getLastOffset(topic: String) = {
    Try {
      val path = topicHistoryDir(topic)
      val dirs = path.listFiles().filter(_.isDirectory)
      val dateDirs = dirs.map(file => dateFormat.parse(file.getName))
      dateFormat.format(dateDirs.max)
    }
  }

  private def readProcedureResult(topic: String, file: File): ProcedureResult = {
    ProcedureResult(topic, readCSVFile(file))
  }

  private def readCSVFile(file: File): List[Partition] = {
    val bufferedSource = io.Source.fromFile(file)
    val result = bufferedSource.getLines().foldLeft(List.empty[Partition])(
      (lst, line) => partitionFromLine(line) :: lst)
    bufferedSource.close()

    result
  }

  private def partitionFromLine(line: String): Partition = {
    val cols = line.split(",")
    Partition(cols(0).toInt, cols(1).toLong)
  }

}
