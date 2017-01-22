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
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.Future
import scala.xml.XML

object SearchService {

  def props(implicit timeout: Timeout) = Props(new SearchService)
  def name = "reportService"

  case class Summary(domain: String, count: Int)
  case class Summaries(summaries: Vector[Summary])

  case class GetSummaries(queries: Set[String])

  case class GetReport(connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]], queries: Set[String])

  class GetResponse(pipe: ActorRef) extends Actor {

    val log = Logging(context.system, this)

    implicit val materializer = ActorMaterializer()

    def receive = {
      case future: Future[HttpResponse] => {
        val originSender = sender
        future.onComplete {
          case Success(result) => {
            result.entity.dataBytes.map(_.utf8String).runReduce(_ + _)
              .onComplete {
                case Success(body) => originSender ! (XML.loadString(body) \\ "item" \\ "link").map(_.text).toList
                case Failure(ex) => {
                  log.error(ex, "Error parsing response")
                  originSender ! None
                }
              }
          }
          case Failure(ex) => {
            log.error(ex, "Error getting response")
            originSender ! None
          }
        }
      }
    }
  }
}

class SearchService(implicit timeout: Timeout) extends Actor {

  import SearchService._

  implicit val materializer = ActorMaterializer()

  val router = context.actorOf(
    BalancingPool(5).props(Props(new GetResponse(self))),
    "poolRouter"
  )

  def receive = {
    case GetReport(connectionFlow, queries) => {

//      val uri = s"/blogs/rss/search?text=${queries.head}&numdoc=10"
//
//        val responseFuture: Future[HttpResponse] =
//          Source.single(HttpRequest(uri = uri))
//            .via(connectionFlow)
//            .runWith(Sink.head)
//
//        responseFuture.onComplete {
//          case Success(result) => {
//            result.entity.dataBytes
//              .map(_.utf8String)
//              .runReduce(_ + _)
//              .onComplete {
//                case Success(body) => {
//                  val xml = XML.loadString(body)
//                  val links = (xml \\ "item" \\ "link").map(_.text).toVector
//
//                  print(links)
//
//                  body
//                }
//                case Failure(fail) => ""
//              }
//          }
//          case Failure(fail) => println("fail" + fail)
//        }

      queries.map(query => s"/blogs/rss/search?text=${query}&numdoc=10")
        .map(uri => router.ask(Source.single(HttpRequest(uri = uri)).via(connectionFlow).runWith(Sink.head)).mapTo[List[String]])
        .foreach(data => data.onComplete {
            case Success(data) => println(data)
            case Failure(fail) => println("Error :" + fail)
        })

      sender ! Summaries(Vector(Summary("default.net", 5)))
    }


    case GetSummaries(queries) => {
      queries.toList match {
        case Nil => sender ! Summaries(Vector(Summary("default.net", 5)))
        case query :: Nil => sender ! Summaries(Vector(Summary(query + ".com", 5)))
        case multiple => sender ! Summaries(multiple.map(query => Summary(query + ".org", 5)).toVector)
      }

    }
  }
}