package com.github.cb372.finagle.beanstalk.protocol

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import java.io.ByteArrayOutputStream

/**
 * Beanstalk protocol spec:
 * https://github.com/kr/beanstalkd/blob/master/doc/protocol.txt
 */

class CommandSpec extends FlatSpec with ShouldMatchers {
  /*
  * Names, in this protocol, are ASCII strings. They may contain letters (A-Z and
  * a-z), numerals (0-9), hyphen ("-"), plus ("+"), slash ("/"), semicolon (";"),
  * dot ("."), dollar-sign ("$"), underscore ("_"), and parentheses ("(" and ")"),
  * but they may not begin with a hyphen.
  */
  val validTubeName = "AZaz09-+/;.$_()"

  val charset = "UTF-8"

  behavior of "put"

  it should "not allow a negative priority" in {
    intercept[IllegalArgumentException] {
      Put(-1, 2, 3, "abcd".getBytes(charset))
    }
  }

  it should "not allow a negative delay" in {
    intercept[IllegalArgumentException] {
      Put(1, -1, 3, "abcd".getBytes(charset))
    }
  }

  it should "not allow a negative timeToRun" in {
    intercept[IllegalArgumentException] {
      Put(1, 2, -1, "abcd".getBytes(charset))
    }
  }

  it should "allow a timeToRun of zero" in {
    /*
     * From the spec:
     * "If the client sends 0, the server will silently increase the ttr to 1."
     */
    Put(1, 2, 0, "abcd".getBytes(charset))
  }

  it should "be encoded correctly" in {
    val cmd = Put(1, 2, 3, "abcd".getBytes(charset))
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("put 1 2 3 4\r\nabcd\r\n")
  }

  behavior of "use"

  itShouldValidateTubeName(Use(_))

  it should "be encoded correctly" in {
    val cmd = Use(validTubeName)
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("use "+validTubeName+"\r\n")
  }

  behavior of "reserve"

  it should "be encoded correctly" in {
    val cmd = Reserve
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("reserve\r\n")
  }

  behavior of "reserve-with-timeout"

  it should "not allow a negative timeout" in {
    intercept[IllegalArgumentException] {
      ReserveWithTimeout(-1)
    }
  }

  it should "allow a zero timeout" in {
    /*
     * A timeout value of 0 will cause the server to immediately return either a
     * response or TIMED_OUT.
     */
    ReserveWithTimeout(0)
  }

  it should "be encoded correctly" in {
    val cmd = ReserveWithTimeout(5)
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("reserve-with-timeout 5\r\n")
  }

  behavior of "delete"

  it should "be encoded correctly" in {
    val cmd = Delete(1234)
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("delete 1234\r\n")
  }

  behavior of "release"

  it should "not allow a negative priority" in {
    intercept[IllegalArgumentException] {
      Release(1, -1, 2)
    }
  }

  it should "not allow a negative delay" in {
    intercept[IllegalArgumentException] {
      Release(1, 2, -1)
    }
  }

  it should "be encoded correctly" in {
    val cmd = Release(10, 20, 30)
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("release 10 20 30\r\n")
  }

  behavior of "bury"

  it should "not allow a negative priority" in {
    intercept[IllegalArgumentException] {
      Bury(1, -1)
    }
  }

  it should "be encoded correctly" in {
    val cmd = Bury(10, 20)
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("bury 10 20\r\n")
  }

  "touch" should "be encoded correctly" in {
    val cmd = Touch(10)
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("touch 10\r\n")
  }

  behavior of "watch"

  itShouldValidateTubeName(Watch(_))

  it should "be encoded correctly" in {
    new String(Watch(validTubeName).toByteArray, charset) should be ("watch "+validTubeName+"\r\n")
  }

  behavior of "ignore"

  itShouldValidateTubeName(Ignore(_))

  it should "be encoded correctly" in {
    val cmd = Ignore(validTubeName)
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("ignore "+validTubeName+"\r\n")
  }

  "peek" should "be encoded correctly" in {
    new String(Peek(5).toByteArray, charset) should be("peek 5\r\n")
  }

  "peek-ready" should "be encoded correctly" in {
    new String(PeekReady.toByteArray, charset) should be("peek-ready\r\n")
  }

  "peek-delayed" should "be encoded correctly" in {
    new String(PeekDelayed.toByteArray, charset) should be("peek-delayed\r\n")
  }

  "peek-buried" should "be encoded correctly" in {
    new String(PeekBuried.toByteArray, charset) should be("peek-buried\r\n")
  }

  behavior of "kick"

  it should "not allow a negative upper bound" in {
    intercept[IllegalArgumentException] {
      Kick(-1)
    }
  }

  it should "be encoded correctly" in {
    new String(Kick(5).toByteArray, charset) should be("kick 5\r\n")
  }

  "stats-job" should "be encoded correctly" in {
    new String(StatsJob(5).toByteArray, charset) should be("stats-job 5\r\n")
  }

  behavior of "stats-tube"

  it should "be encoded correctly" in {
    new String(StatsTube(validTubeName).toByteArray, charset) should be("stats-tube "+validTubeName+"\r\n")
  }

  "stats" should "be encoded correctly" in {
    new String(Stats.toByteArray, charset) should be("stats\r\n")
  }

  "list-tubes" should "be encoded correctly" in {
    new String(ListTubes.toByteArray, charset) should be("list-tubes\r\n")
  }

  "list-tube-used" should "be encoded correctly" in {
    new String(ListTubeUsed.toByteArray, charset) should be("list-tube-used\r\n")
  }

  "list-tubes-watched" should "be encoded correctly" in {
    new String(ListTubesWatched.toByteArray, charset) should be("list-tubes-watched\r\n")
  }

  "quit" should "be encoded correctly" in {
    new String(Quit.toByteArray, charset) should be("quit\r\n")
  }

  behavior of "pause-tube"

  it should "not allow a negative delay" in {
    intercept[IllegalArgumentException] {
      PauseTube(validTubeName, -1)
    }
  }

  itShouldValidateTubeName(PauseTube(_, 5))

  it should "be encoded correctly" in {
    new String(PauseTube(validTubeName, 5).toByteArray, charset) should be("pause-tube "+validTubeName+" 5\r\n")
  }


  private def itShouldValidateTubeName(cmdMaker: String => Command) {

    it should "not allow a tube name longer than 200 chars" in {
      val longTubeName = "a" * 201
      intercept[IllegalArgumentException] {
        cmdMaker(longTubeName)
      }
    }

    it should "not allow a tube name starting with a hyphen" in {
      intercept[IllegalArgumentException] {
        cmdMaker("-a")
      }
    }

    it should "allow all the chars specified in the spec" in {
      cmdMaker(validTubeName)
    }

    it should "not allow chars except those specified in the spec" in {
      intercept[IllegalArgumentException] {
        cmdMaker("-a")
      }
    }

  }
}
