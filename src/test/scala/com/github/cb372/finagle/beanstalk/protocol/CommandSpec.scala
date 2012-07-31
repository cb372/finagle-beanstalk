package com.github.cb372.finagle.beanstalk.protocol

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import java.io.ByteArrayOutputStream

/**
 * Beanstalk protocol spec:
 * https://github.com/kr/beanstalkd/blob/master/doc/protocol.txt
 *
 * Author: chris
 * Created: 7/30/12
 */

class CommandSpec extends FlatSpec with ShouldMatchers {

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

  it should "not allow a tube name longer than 200 chars" in {
    val longTubeName = "a" * 201
    intercept[IllegalArgumentException] {
      Use(longTubeName)
    }
  }

  it should "not allow a tube name starting with a hyphen" in {
    intercept[IllegalArgumentException] {
      Use("-a")
    }
  }

  it should "allow all the chars specified in the spec" in {
    /*
     * Names, in this protocol, are ASCII strings. They may contain letters (A-Z and
     * a-z), numerals (0-9), hyphen ("-"), plus ("+"), slash ("/"), semicolon (";"),
     * dot ("."), dollar-sign ("$"), underscore ("_"), and parentheses ("(" and ")"),
     * but they may not begin with a hyphen.
     */
    val tubeName = "AZaz09-+/;.$_()"
    Use(tubeName)
  }

  it should "not allow chars except those specified in the spec" in {
    intercept[IllegalArgumentException] {
      Use("-a")
    }
  }

  it should "be encoded correctly" in {
    val tubeName = "AZaz09-+/;.$_()"
    val cmd = Use(tubeName)
    val encoded = cmd.toByteArray

    new String(encoded, charset) should be ("use "+tubeName+"\r\n")
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
}
