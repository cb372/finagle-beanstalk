package com.github.cb372.finagle.beanstalk.protocol

import com.github.cb372.finagle.beanstalk.naggati.{Incomplete, GoToStage, Emit, NextStep}
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.scalatest.{Matchers, FlatSpec}

import scala.annotation.tailrec

/**
  * Beanstalk protocol spec:
  * https://github.com/kr/beanstalkd/blob/master/doc/protocol.txt
  */

class ReplySpec extends FlatSpec with Matchers {

  val charset = "UTF-8"

  behavior of "ReplyDecoder"

  /*
   * One-line replies
   */

  it should "decode INSERTED" in {
    checkDecode("INSERTED 123\r\n", Inserted(123))
  }

  it should "decode BURIED without an ID" in {
    checkDecode("BURIED\r\n", Buried)
  }

  it should "decode BURIED with an ID" in {
    checkDecode("BURIED 234\r\n", Buried(234))
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
    checkDecode("DEADLINE_SOON\r\n", DeadlineSoon)
  }

  it should "decode TIMED_OUT" in {
    checkDecode("TIMED_OUT\r\n", TimedOut)
  }

  it should "decode RELEASED" in {
    checkDecode("RELEASED\r\n", Released)
  }

  it should "decode TOUCHED" in {
    checkDecode("TOUCHED\r\n", Touched)
  }

  it should "decode NOT_FOUND" in {
    checkDecode("NOT_FOUND\r\n", NotFound)
  }

  it should "decode WATCHING" in {
    checkDecode("WATCHING 100\r\n", Watching(100))
  }

  it should "decode NOT_IGNORED" in {
    checkDecode("NOT_IGNORED\r\n", NotIgnored)
  }

  it should "decode KICKED" in {
    checkDecode("KICKED 5\r\n", Kicked(5))
  }

  it should "decode PAUSED" in {
    checkDecode("PAUSED\r\n", Paused)
  }

  /*
   * Replies with data
   */

  it should "decode RESERVED" in {
    val bytes = "RESERVED 1234 5\r\nabcde\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted.getClass should be (classOf[Reserved])
    emitted.asInstanceOf[Reserved].id should be (1234)
    new String(emitted.asInstanceOf[Reserved].data, charset) should be ("abcde")
  }


  it should "decode multi-byte data" in {
    val string = "あいう漢字"
    val data = string.getBytes(charset)
    val bytes = Array.concat(
      ("RESERVED 1234 "+data.length+"\r\n").getBytes(charset),
      data,
      "\r\n".getBytes(charset)
    )
    val emitted = readUntilEmit(bytes)

    emitted.getClass should be (classOf[Reserved])
    emitted.asInstanceOf[Reserved].id should be (1234)
    new String(emitted.asInstanceOf[Reserved].data, charset) should be (string)
  }

  it should "decode FOUND" in {
    val bytes = "FOUND 2345 7\r\n   a   \r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted.getClass should be (classOf[Found])
    emitted.asInstanceOf[Found].id should be (2345)
    new String(emitted.asInstanceOf[Found].data, charset) should be ("   a   ")
  }

  it should "decode OK" in {
    val bytes = "OK 14\r\nsome yaml data\r\n".getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted.getClass should be (classOf[Ok])
    new String(emitted.asInstanceOf[Ok].data, charset) should be ("some yaml data")
  }


  /*
  * Error replies
  */

  it should "decode EXPECTED_CRLF" in {
    checkDecode("EXPECTED_CRLF\r\n", ExpectedCrLf)
  }

  it should "decode JOB_TOO_BIG" in {
    checkDecode("JOB_TOO_BIG\r\n", JobTooBig)
  }

  it should "decode DRAINING" in {
    checkDecode("DRAINING\r\n", Draining)
  }

  it should "decode OUT_OF_MEMORY" in {
    checkDecode("OUT_OF_MEMORY\r\n", OutOfMemory)
  }

  it should "decode INTERNAL_ERROR" in {
    checkDecode("INTERNAL_ERROR\r\n", InternalError)
  }

  it should "decode BAD_FORMAT" in {
    checkDecode("BAD_FORMAT\r\n", BadFormat)
  }

  it should "decode UNKNOWN_COMMAND" in {
    checkDecode("UNKNOWN_COMMAND\r\n", UnknownCommand)
  }

  it should "produce an UnsupportedReplyError if it doesn't understand the server's reply" in {
    checkDecode("OH MY GOD!!\r\n", UnsupportedReply("OH MY GOD!!"))
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

  private def checkDecode(raw: String, expected: Reply) {
    val bytes = raw.getBytes(charset)
    val emitted = readUntilEmit(bytes)
    emitted should be (expected)
  }

}
