package com.github.cb372.finagle.beanstalk.protocol

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import com.twitter.naggati._
import com.twitter.naggati.GoToStage
import com.twitter.naggati.Emit
import annotation.tailrec

/**
  * Beanstalk protocol spec:
  * https://github.com/kr/beanstalkd/blob/master/doc/protocol.txt
  *
  * Author: chris
  * Created: 7/30/12
  */

class ReplySpec extends FlatSpec with ShouldMatchers {

  val charset = "UTF-8"

  behavior of "ReplyDecoder"

  it should "produce an UnsupportedReplyError if it doesn't understand the server's reply" in {
    val bytes = "OH MY GOD!!\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (UnsupportedReply("OH MY GOD!!"))
  }

  it should "decode INSERTED" in {
    val bytes = "INSERTED 123\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (Inserted(123))
  }

  it should "decode BURIED" in {
    val bytes = "BURIED 234\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (Buried(234))
  }

  it should "decode EXPECTED_CRLF" in {
    val bytes = "EXPECTED_CRLF\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (ExpectedCrLf)
  }

  it should "decode JOB_TOO_BIG" in {
    val bytes = "JOB_TOO_BIG\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (JobTooBig)
  }

  it should "decode DRAINING" in {
    val bytes = "DRAINING\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (Draining)
  }

  it should "decode USING" in {
    /*
     * Names, in this protocol, are ASCII strings. They may contain letters (A-Z and
     * a-z), numerals (0-9), hyphen ("-"), plus ("+"), slash ("/"), semicolon (";"),
     * dot ("."), dollar-sign ("$"), underscore ("_"), and parentheses ("(" and ")"),
     * but they may not begin with a hyphen.
     */
    val tubeName = "AZaz09-+/;.$_()"
    val bytes = ("USING "+tubeName+"\r\n").getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (Using(tubeName))
  }

  it should "decode DEADLINE_SOON" in {
    val bytes = "DEADLINE_SOON\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (DeadlineSoon)
  }

  it should "decode TIMED_OUT" in {
    val bytes = "TIMED_OUT\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (TimedOut)
  }

  it should "decode RESERVED" in {
    val bytes = "RESERVED 1234 5\r\nabcde\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted.getClass should be (classOf[Reserved])
    emitted.asInstanceOf[Reserved].id should be (1234)
    new String(emitted.asInstanceOf[Reserved].data, charset) should be ("abcde")
  }

  def readUntilEmit(bytes: Array[Byte]): AnyRef = {
    val buff = ChannelBuffers.wrappedBuffer(bytes)
    val nextStep = ReplyDecoder.decode.apply(buff)
    readUntilEmit(buff, nextStep)
  }

  @tailrec
  private def readUntilEmit(buff: ChannelBuffer, nextStep: NextStep): AnyRef = nextStep match {
    case Emit(obj) => obj
    case GoToStage(stage) => readUntilEmit(buff, stage.apply(buff))
    case Incomplete => fail("Incomplete")
  }

}
