package com.ntrlab.test.actors

import java.io.File
import java.text.SimpleDateFormat

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor}
import com.ntrlab.test.actors.DirWorker.{TopicLastRunStat, GetTopics}
import com.ntrlab.test.entity.{LastRunStats, Partition, ProcedureResult}

import scala.util.{Failure, Success, Try}

object DirWorker {
  def props(path: String) = Props(new DirWorker(path))

  case object GetTopics

  case class TopicLastRunStat(topic: String)

}

class DirWorker(path: String) extends Actor {

  override def receive: Receive = {
    case GetTopics =>
      val testList = List("topic1", "topic2", "topic3")
      sender ! testList.map(name => ProcedureResult(name, List.empty))

    case TopicLastRunStat(topic) => sender ! lastRunStats(getTopicStats(topic)(getLastOffsetFile(topic)))

  }

  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss")

  private def topicHistoryFile(topic: String) = new File(path, s"$topic/history")

  private val offset = "offset.csv"

  private def lastRunStats(procRes: Option[ProcedureResult]): Option[LastRunStats] = {
    procRes.map(res => {
      val messages = res.stats.map(_.messages)
      val sum = messages.sum
      LastRunStats(
        sum,
        messages.min,
        messages.max,
        sum / messages.length
      )
    })
  }

  private def getTopicStats(topic: String)(offsetFile: Try[File]): Option[ProcedureResult] = {
    offsetFile match {
      case Success(file) => Some(ProcedureResult(topic, readCSVFile(file)))
      case Failure(e) => None
    }
  }

  private def getLastOffsetFile(topic: String): Try[File] = {
    Try {
      val path = topicHistoryFile(topic)
      val dirs = path.listFiles().filter(_.isDirectory)
      val parsedDirs = dirs.map(file => dateFormat.parse(file.getName))
      new File(path, s"${dateFormat.format(parsedDirs.max)}/$offset")
    }
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
