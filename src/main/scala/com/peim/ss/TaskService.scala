package com.peim.ss

import akka.actor.{Actor, Props}
import akka.util.Timeout

object SummaryService {

  def props(implicit timeout: Timeout) = Props(new SummaryService)
  def name = "summaryService"

  case class Summary(domain: String, count: Int)
  case class Summaries(summaries: Vector[Summary])

  case class GetSummaries(param: String)
}

class SummaryService(implicit timeout: Timeout) extends Actor {

  import SummaryService._

  def receive = {
    case GetSummaries(param) => {
      val domain = param + ".com"
      sender ! Summaries(Vector(Summary(domain, 5)))
    }
  }
}