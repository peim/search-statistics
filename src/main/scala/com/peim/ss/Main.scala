package com.peim.ss

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

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
}


