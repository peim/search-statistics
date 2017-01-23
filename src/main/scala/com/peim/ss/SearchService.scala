package com.peim.ss

import java.net.URL

import akka.actor.{Actor, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.pattern.ask
import akka.routing.BalancingPool
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object SearchService {

  def props(implicit timeout: Timeout) = Props(new SearchService)
  def name = "searchService"

  case class Report(report: Map[String, Int])
  case class GetReport(connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]], queries: Set[String])
}

class SearchService(implicit timeout: Timeout) extends Actor {

  import SearchService._

  implicit val materializer = ActorMaterializer()

  val config = ConfigFactory.load()
  val conCount = config.getInt("pool.connection-count")
  val name = config.getString("pool.name")

  val log = Logging(context.system, this)
  val router = context.actorOf(
    BalancingPool(conCount).props(Props(new RequestHandler(self))),
    name
  )

  def receive = {
    case GetReport(connectionFlow, queries) => {
      val originSender = sender
      val requestFutures = queries.map(query => s"/blogs/rss/search?text=${query}&numdoc=10")
        .map(uri => router.ask(
          Source.single(HttpRequest(uri = uri))
            .via(connectionFlow)
            .runWith(Sink.head)
        ).mapTo[List[String]])

      Future.reduce(requestFutures)(_ ::: _).onComplete {
        case Success(links) => {
          originSender ! Report(links.distinct
            .map((link: String) => new URL(link).getHost)
            .groupBy(domain => domain)
            .map(key => key._1 -> key._2.size))
        }
        case Failure(ex) => {
          log.error(ex, "Error getting response")
          originSender ! None
        }
      }
    }
  }
}