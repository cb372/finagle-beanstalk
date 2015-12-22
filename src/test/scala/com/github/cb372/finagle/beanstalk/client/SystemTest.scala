package com.github.cb372.finagle.beanstalk.client

import org.scalatest.fixture.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import com.github.cb372.finagle.beanstalk.protocol.{Reserved, Inserted, Put}

import sys.process._
import util.Random

/**
 * Author: chris
 * Created: 7/29/12
 */

class SystemTest extends FlatSpec with ShouldMatchers {
  val charset = "UTF-8"

  behavior of "beanstalk client"

  it should "send a put to beanstalkd and receive an INSERTED reply" in { port =>
    val client = BeanstalkClient.build("localhost:"+port)
    val reply = client.put("hello", PutOpts()).get()
    val jobId = reply.right.get
    jobId should be >= 0
  }

  it should "be able to reserve a job inserted by another client" in { port =>
    val client1 = BeanstalkClient.build("localhost:"+port)
    val client2 = BeanstalkClient.build("localhost:"+port)

    val (putReply, reserveReply) =
      ((client1.put("hello", PutOpts())) join (client2.reserve())).get()

    val insertedId = putReply.right.get
    val reservedJob = reserveReply.right.get

    reservedJob.id should be (insertedId)
    new String(reservedJob.data, charset) should be ("hello")
  }

//  it should "be able to retrieve stats" in { port =>
//    val client = BeanstalkClient.build("localhost:"+port)
//    val reply = client.stats().get()
//    val stats = reply.right.get
//    println(stats)
//  }

  type FixtureParam = Int

  override def withFixture(test: OneArgTest) {
    val server = startBeanstalkdServer()
    server match {
      case Some((proc, port)) => {
        try {
          test(port)
        } finally {
          // shutdown server
          proc.destroy()
        }
      }
      case _ => {
        info("WARN: Skipping test because beanstalkd is not on the PATH")
      }
    }
  }

  private def startBeanstalkdServer(): Option[(Process, Int)] = {
    val beanstalkdCmd = sys.env.getOrElse("BEANSTALKD", "beanstalkd")
    try {
      val versionString = beanstalkdCmd + " -v".!!
      if (versionString.startsWith("beanstalkd")) {
        // looks OK, let's start the server on a random port
        val port = 11300 + Random.nextInt(1000)
        val cmd = beanstalkdCmd + " -p " + port
        Some(cmd.run(), port)
      } else {
        println("Expected a beanstalkd version string but got: " + versionString)
        None
      }
    } catch {
      case e: Throwable => None
    }
  }
}
