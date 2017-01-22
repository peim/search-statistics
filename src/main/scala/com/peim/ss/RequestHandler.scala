package com.peim.ss

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import akka.http.scaladsl.model.HttpResponse
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.xml.XML

import scala.concurrent.ExecutionContext.Implicits.global

class RequestHandler(pipe: ActorRef) extends Actor {

  implicit val materializer = ActorMaterializer()

  val log = Logging(context.system, this)

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