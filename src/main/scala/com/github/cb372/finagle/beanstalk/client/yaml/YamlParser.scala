package com.github.cb372.finagle.beanstalk.client.yaml

trait YamlParser {

  def parseList(yaml: String): Seq[String]

  def parseMap(yaml: String): Map[String, String]

}
