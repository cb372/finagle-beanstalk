package com.github.cb372.finagle.beanstalk.client.yaml

/**
 * Author: chris
 * Created: 8/8/12
 */

trait YamlParser {

  def parseList(yaml: String): Seq[String]

  def parseMap(yaml: String): Map[String, String]

}
