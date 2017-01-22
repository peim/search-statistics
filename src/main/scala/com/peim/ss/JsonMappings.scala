package com.peim.ss

import spray.json._

trait JsonMappings extends DefaultJsonProtocol {
  import SearchService._

  implicit val reportFormat = jsonFormat1(Report)
}

