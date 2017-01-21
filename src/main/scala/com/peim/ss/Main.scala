package com.peim.ss

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import scala.xml._


import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends App with RequestTimeout {

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher

  val restApi = new RestApi(system, requestTimeout(config)).routes

  implicit val materializer = ActorMaterializer()
  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(restApi, host, port)

  val log = Logging(system.eventStream, "search-statistics")
  bindingFuture.map { serverBinding =>
    log.info(s"RestApi bound to ${serverBinding.localAddress} ")
  }.onFailure {
    case ex: Exception =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  }

  val responseFuture =
    Http.get(system).singleRequest(HttpRequest.apply(
      HttpMethods.GET,
      "http://yandex.ru/blogs/rss/search?text=scala&numdoc=10"
    ));

  responseFuture.onComplete {
    case Success(result) => {
      val res = result.entity.dataBytes
        .map(_.utf8String)
        .runReduce(_ + _)
        .onComplete {
        case Success(body) => {
          val xml = scala.xml.XML.loadString(body)
          val links = (xml \\ "item" \\ "link").map(_.text).toVector

          print(links)

          body
        }
        case Failure(fail) => ""
      }
    }
    case Failure(fail) => println("fail" + fail)
  }
}


