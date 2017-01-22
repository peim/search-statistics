package com.peim.ss

import scala.concurrent.ExecutionContext
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.peim.ss.SearchService._
import com.peim.ss.SearchService.{GetSummaries}

class RestApi(system: ActorSystem, timeout: Timeout) extends RestRoutes {
  implicit val requestTimeout = timeout
  implicit val executionContext = system.dispatcher

  def createSearchService = system.actorOf(SearchService.props, SearchService.name)
}

trait RestRoutes extends SearchServiceApi with JsonMappings {

  import StatusCodes._

  def routes: Route = path("search") {
    get {
      parameters('query.as[String].*) { queries =>
        onSuccess(getSummaries(queries.toSet)) { summaries =>
          complete(OK, summaries)
        }
      }
    }
  }
}

trait SearchServiceApi {

  def createSearchService(): ActorRef

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val searchService = createSearchService()

  def getSummaries(queries: Set[String]) = searchService.ask(GetSummaries(queries)).mapTo[Summaries]
}

