package com.github.cb372.finagle.beanstalk.protocol

import com.twitter.naggati.{Stages, Encoder}

/**
 * Author: chris
 * Created: 7/27/12
 */

/** A reply from the server in response to a command */
sealed trait Reply

/** Result generated when the server sends a reply that we do not understand */
case class UnsupportedReply(line: String) extends Reply

/** Marker trait to signal some kind of error */
sealed trait ErrorReply extends Reply

case class Inserted(id: Int) extends Reply

case class Buried(id: Int) extends Reply

case object ExpectedCrLf extends ErrorReply

case object JobTooBig extends ErrorReply

case object Draining extends ErrorReply

case class Using(tube: String) extends Reply

case object DeadlineSoon extends Reply

case object TimedOut extends Reply

case class Reserved(id: Int, data: Array[Byte]) extends Reply


object Replies {
  val INSERTED = "INSERTED"
  val BURIED = "BURIED"
  val EXPECTED_CRLF = "EXPECTED_CRLF"
  val JOB_TOO_BIG = "JOB_TOO_BIG"
  val DRAINING = "DRAINING"
  val USING = "USING"
  val DEADLINE_SOON = "DEADLINE_SOON"
  val TIMED_OUT = "TIMED_OUT"
  val RESERVED = "RESERVED"

}

object ReplyDecoder {
  import BeanstalkCodec._
  import Replies._
  import com.twitter.naggati.Stages._

  val decode = readLine(removeLF = true, encoding = CHARSET) { line =>
    line.split(TOKEN_DELIMITER).toList match {
      case INSERTED :: id :: _ => emit(Inserted(id.trim.toInt))
      case BURIED :: id :: _ => emit(Buried(id.trim.toInt))
      case EXPECTED_CRLF :: _ => emit(ExpectedCrLf)
      case JOB_TOO_BIG :: _ => emit(JobTooBig)
      case DRAINING :: _ => emit(Draining)
      case USING :: xs => {
        val tube = xs.mkString(TOKEN_DELIMITER.toString)
        emit(Using(tube))
      }
      case DEADLINE_SOON :: _ => emit(DeadlineSoon)
      case TIMED_OUT :: _ => emit(TimedOut)
      case RESERVED :: id :: bytes :: _ => readBytes(bytes.trim.toInt) { bytes =>
        // consume the newline after the data
        readBytes(2) { _ =>
          emit(Reserved(id.trim.toInt, bytes))
        }
      }
      case _ => emit(UnsupportedReply(line))
    }
  }

}
