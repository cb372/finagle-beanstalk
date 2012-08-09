package com.github.cb372.finagle.beanstalk.client.yaml

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

/**
 * Author: chris
 * Created: 8/8/12
 */

class StoopidYamlParserSpec extends FlatSpec with ShouldMatchers {

  behavior of "StoopidYamlParser"

  it should "be able to parse a list of tubes" in {
    val parser = new StoopidYamlParser
    parser.parseList(yamlTubeList) should be (Seq("default", "foo", "bar"))
  }

  it should "be able to parse stats as a map" in {
    val parser = new StoopidYamlParser
    val map = parser.parseMap(yamlStats)

    map.size should be (46)
    map("current-jobs-urgent") should be("0")
    map("max-job-size") should be("65535")
    map("version") should be("1.6+2+gecb403e")
    map("binlog-max-size") should be("10485760")
  }

  val yamlTubeList =
    """---
      |- default
      |- foo
      |- bar
    """.stripMargin

  val yamlStats =
    """---
      |current-jobs-urgent: 0
      |current-jobs-ready: 0
      |current-jobs-reserved: 0
      |current-jobs-delayed: 0
      |current-jobs-buried: 0
      |cmd-put: 0
      |cmd-peek: 0
      |cmd-peek-ready: 0
      |cmd-peek-delayed: 0
      |cmd-peek-buried: 0
      |cmd-reserve: 0
      |cmd-reserve-with-timeout: 0
      |cmd-delete: 0
      |cmd-release: 0
      |cmd-use: 0
      |cmd-watch: 0
      |cmd-ignore: 0
      |cmd-bury: 0
      |cmd-kick: 0
      |cmd-touch: 0
      |cmd-stats: 1
      |cmd-stats-job: 0
      |cmd-stats-tube: 0
      |cmd-list-tubes: 0
      |cmd-list-tube-used: 0
      |cmd-list-tubes-watched: 0
      |cmd-pause-tube: 0
      |job-timeouts: 0
      |total-jobs: 0
      |max-job-size: 65535
      |current-tubes: 1
      |current-connections: 1
      |current-producers: 0
      |current-workers: 0
      |current-waiting: 0
      |total-connections: 1
      |pid: 74710
      |version: 1.6+2+gecb403e
      |rusage-utime: 0.002007
      |rusage-stime: 0.002725
      |uptime: 0
      |binlog-oldest-index: 0
      |binlog-current-index: 0
      |binlog-records-migrated: 0
      |binlog-records-written: 0
      |binlog-max-size: 10485760
    """.stripMargin

}
