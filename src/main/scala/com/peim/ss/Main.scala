package com.peim.ss

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.{ByteString, Timeout}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.xml.XML
import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem("search-statistics")
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(30 seconds)

  val restApi = new RestApi(system, timeout).routes
  val log = Logging(system.eventStream, "search-statistics")

  Http().bindAndHandle(restApi, host, port).map {
    serverBinding => log.info(s"RestApi bound to ${serverBinding.localAddress} ")
  }.onFailure {
    case ex: Exception =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  }


//  val uri = "/blogs/rss/search?text=scala&numdoc=10"
//
//  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
//    Http().outgoingConnection("yandex.ru")
//  val responseFuture: Future[HttpResponse] =
//    Source.single(HttpRequest(uri = uri))
//      .via(connectionFlow)
//      .runWith(Sink.head)
//
//  responseFuture.onComplete {
//    case Success(result) => {
//      result.entity.dataBytes
//        .map(_.utf8String)
//        .runReduce(_ + _)
//        .onComplete {
//          case Success(body) => {
//            val xml = XML.loadString(body)
//            val links = (xml \\ "item" \\ "link").map(_.text).toVector
//
//            print(links)
//
//            body
//          }
//          case Failure(fail) => ""
//        }
//    }
//    case Failure(fail) => println("fail" + fail)
//  }
}


