package com.peim.ss

import scala.concurrent.{ExecutionContext, Future}
import akka.actor._
import akka.http.scaladsl.Http
import akka.pattern.ask
import akka.util.Timeout
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Flow
import com.peim.ss.SearchService._

class RestApi(system: ActorSystem, timeout: Timeout) extends RestRoutes {
  implicit val actorSystem = system
  implicit val requestTimeout = timeout
  implicit val executionContext = system.dispatcher

  def createSearchService = system.actorOf(SearchService.props, SearchService.name)
  def createConnectionFlow = Http().outgoingConnection("yandex.ru")
}

trait RestRoutes extends SearchServiceApi with JsonMappings {

  import StatusCodes._

  def routes: Route = path("search") {
    get {
      parameters('query.as[String].*) { queries =>
        onSuccess(getReport(queries.toSet)) { report =>
          complete(OK, report)
        }
      }
    }
  }
}

trait SearchServiceApi {

  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val searchService = createSearchService()
  lazy val connectionFlow = createConnectionFlow()

  def createSearchService(): ActorRef
  def createConnectionFlow(): Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]]

  def getReport(queries: Set[String]) =
    searchService.ask(GetReport(connectionFlow, queries)).mapTo[Summaries]
}

