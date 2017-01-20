package com.peim.ss

import akka.actor.{Actor, Props}
import akka.util.Timeout

object SummaryService {

  def props(implicit timeout: Timeout) = Props(new SummaryService)
  def name = "summaryService"

  case class Summary(domain: String, count: Int)
  case class Summaries(summaries: Vector[Summary])

  case class GetSummaries(queries: List[String])
}

class SummaryService(implicit timeout: Timeout) extends Actor {

  import SummaryService._

  def receive = {
    case GetSummaries(queries) => {
      queries match {
        case Nil => sender ! Summaries(Vector(Summary("default.net", 5)))
        case query :: Nil => sender ! Summaries(Vector(Summary(query + ".com", 5)))
        case multiple => sender ! Summaries(multiple.map(query => Summary(query + ".org", 5)).toVector)
      }

  }
}
}