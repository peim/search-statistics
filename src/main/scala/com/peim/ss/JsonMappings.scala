package com.peim.ss

import spray.json._

trait JsonMappings extends DefaultJsonProtocol {
  import SearchService._

  implicit val summaryFormat = jsonFormat2(Summary)
  implicit val summariesFormat = jsonFormat1(Summaries)
}

