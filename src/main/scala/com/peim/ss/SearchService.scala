package com.peim.ss

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.routing.BalancingPool
import akka.stream.ActorMaterializer
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.xml.XML

object SearchService {

  def props(implicit timeout: Timeout) = Props(new SearchService)
  def name = "searchService"

  case class Summary(domain: String, count: Int)
  case class Summaries(summaries: Vector[Summary])

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


      sender ! Summaries(Vector(Summary("default.net", 5)))
    }

  }
}