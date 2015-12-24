package com.github.cb372.finagle.beanstalk.client.yaml

import io.Source

class StoopidYamlParser extends YamlParser {

  private val keyValue = "([^:]+): (.*)".r

  def parseList(yaml: String): Seq[String] = {
    Source.fromString(yaml).getLines().toList.collect {
      case s if s.startsWith("- ") => s.substring(2)
    }
  }

  def parseMap(yaml: String): Map[String, String] = {
    val tuples: Seq[(String, String)] =
      Source.fromString(yaml).getLines().toList.collect {
        case keyValue(groups@_*) => (groups(0) -> groups(1))
      }
    tuples.toMap
  }
}
