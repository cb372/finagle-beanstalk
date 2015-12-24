package com.github.cb372.finagle.beanstalk.client

import com.twitter.util.Await
import org.scalatest.{Matchers, Outcome, fixture}

import scala.sys.process._
import scala.util.Random

class SystemTest extends fixture.FlatSpec with Matchers {
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
      Await.result(client1.put("hello", PutOpts()) join client2.reserve())

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

  override def withFixture(test: OneArgTest): Outcome = {
    val (proc, port) = startBeanstalkdServer()

    try {
      test(port)
    } finally {
      // shutdown server
      proc.destroy()
    }
  }

  private def startBeanstalkdServer(): (Process, Int) = {
    val beanstalkdCmd = sys.env.getOrElse("BEANSTALKD", "beanstalkd")
    val versionString = s"$beanstalkdCmd -v".!!
    if (versionString.startsWith("beanstalkd")) {
      // looks OK, let's start the server on a random port
      val port = 11300 + Random.nextInt(1000)
      val cmd = beanstalkdCmd + " -p " + port
      (cmd.run(), port)
    } else {
      throw new IllegalArgumentException("Expected a beanstalkd version string but got: " + versionString)
    }
  }
}
