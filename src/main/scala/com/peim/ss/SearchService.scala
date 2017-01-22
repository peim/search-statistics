package com.peim.ss

import akka.actor.{Actor, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.pattern.ask
import akka.routing.BalancingPool
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

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

  val log = Logging(context.system, this)
  val router = context.actorOf(
    BalancingPool(3).props(Props(new RequestHandler(self))),
    "poolRouter"
  )

  def receive = {
    case GetReport(connectionFlow, queries) => {

      queries.map(query => s"/blogs/rss/search?text=${query}&numdoc=10")
        .map(uri => router.ask(Source.single(HttpRequest(uri = uri)).via(connectionFlow).runWith(Sink.head)).mapTo[List[String]])
        .foreach(data => data.onComplete {
          case Success(data) => println(data)
          case Failure(fail) => println("Error :" + fail)
        })

      sender ! Report(Map("hello.com" -> 5, "vk.com" -> 6))
    }

  }
}