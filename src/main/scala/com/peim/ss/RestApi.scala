package com.peim.ss

import scala.concurrent.ExecutionContext
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.peim.ss.SummaryService._
import com.peim.ss.SummaryService.{GetSummaries}

class RestApi(system: ActorSystem, timeout: Timeout) extends RestRoutes {
  implicit val requestTimeout = timeout
  implicit val executionContext = system.dispatcher

  def createSummaryService = system.actorOf(SummaryService.props, SummaryService.name)
}

trait RestRoutes extends SummaryServiceApi with JsonMappings {

  import StatusCodes._

  def routes: Route = pathPrefix("search") {
    pathEndOrSingleSlash {
      // GET /search
      get {
        onSuccess(getSummaries("hello")) { summaries =>
          complete(OK, summaries)
        }
      }
    }
  }
}

trait SummaryServiceApi {

  def createSummaryService(): ActorRef

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val summaryService = createSummaryService()

  def getSummaries(param: String) = summaryService.ask(GetSummaries(param)).mapTo[Summaries]
}

