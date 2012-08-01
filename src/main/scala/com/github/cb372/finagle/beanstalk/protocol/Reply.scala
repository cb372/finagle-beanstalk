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

/*
 * One-line replies
 */

case class Inserted(id: Int) extends Reply

case object Buried extends Reply

case class Buried(id: Int) extends Reply

case class Using(tube: String) extends Reply

case object DeadlineSoon extends Reply

case object TimedOut extends Reply

case object Deleted extends Reply

case object Released extends Reply

case object Touched extends Reply

case object NotFound extends Reply

case class Watching(count: Int) extends Reply

case object NotIgnored extends Reply

case class Kicked(count: Int) extends Reply

case object Paused extends Reply

/*
 * Replies with data
 */

case class Reserved(id: Int, data: Array[Byte]) extends Reply

case class Found(id: Int, data: Array[Byte]) extends Reply

case class Ok(data: Array[Byte]) extends Reply

/** Marker trait to signal some kind of error */
sealed trait ErrorReply extends Reply

case object ExpectedCrLf extends ErrorReply

case object JobTooBig extends ErrorReply

case object Draining extends ErrorReply

case object OutOfMemory extends ErrorReply

case object InternalError extends ErrorReply

case object BadFormat extends ErrorReply

case object UnknownCommand extends ErrorReply


object Replies {
  val INSERTED = "INSERTED"
  val BURIED = "BURIED"
  val USING = "USING"
  val DEADLINE_SOON = "DEADLINE_SOON"
  val TIMED_OUT = "TIMED_OUT"
  val RESERVED = "RESERVED"
  val DELETED = "DELETED"
  val NOT_FOUND = "NOT_FOUND"
  val RELEASED = "RELEASED"
  val TOUCHED = "TOUCHED"
  val WATCHING = "WATCHING"
  val NOT_IGNORED = "NOT_IGNORED"
  val OK = "OK"
  val FOUND = "FOUND"
  val KICKED = "KICKED"
  val PAUSED = "PAUSED"

  /* Errors */
  val EXPECTED_CRLF = "EXPECTED_CRLF"
  val JOB_TOO_BIG = "JOB_TOO_BIG"
  val DRAINING = "DRAINING"
  val OUT_OF_MEMORY = "OUT_OF_MEMORY"
  val INTERNAL_ERROR = "INTERNAL_ERROR"
  val BAD_FORMAT = "BAD_FORMAT"
  val UNKNOWN_COMMAND = "UNKNOWN_COMMAND"
}

object ReplyDecoder {
  import BeanstalkCodec._
  import Replies._
  import com.twitter.naggati.Stages._

  val decode = readLine(removeLF = true, encoding = CHARSET) { line =>
    line.split(TOKEN_DELIMITER).toList match {
      /*
       * Zero-arg one-line replies
       */
      case BURIED :: Nil => emit(Buried)
      case DEADLINE_SOON :: _ => emit(DeadlineSoon)
      case TIMED_OUT :: _ => emit(TimedOut)
      case DELETED :: _ => emit(Deleted)
      case RELEASED :: _ => emit(Released)
      case TOUCHED :: _ => emit(Touched)
      case NOT_FOUND :: _ => emit(NotFound)
      case NOT_IGNORED :: _ => emit(NotIgnored)
      case PAUSED :: _ => emit(Paused)
      /*
      * One-arg one-line replies
      */
      case INSERTED :: id :: _ => emit(Inserted(id.trim.toInt))
      case BURIED :: id :: _ => emit(Buried(id.trim.toInt))
      case WATCHING :: count :: _ => emit(Watching(count.trim.toInt))
      case KICKED :: count :: _ => emit(Kicked(count.trim.toInt))
      case USING :: xs => {
        val tube = xs.mkString(TOKEN_DELIMITER.toString)
        emit(Using(tube))
      }

      /*
       * Replies with data
       */
      case RESERVED :: id :: bytes :: _ => readBytes(bytes.trim.toInt) { bytes =>
        // consume the newline after the data
        readBytes(2) { _ =>
          emit(Reserved(id.trim.toInt, bytes))
        }
      }
      case FOUND :: id :: bytes :: _ => readBytes(bytes.trim.toInt) { bytes =>
      // consume the newline after the data
        readBytes(2) { _ =>
          emit(Found(id.trim.toInt, bytes))
        }
      }
      case OK :: bytes :: _ => readBytes(bytes.trim.toInt) { bytes =>
      // consume the newline after the data
        readBytes(2) { _ =>
          emit(Ok(bytes))
        }
      }
      /*
       * Error replies
       */
      case EXPECTED_CRLF :: _ => emit(ExpectedCrLf)
      case JOB_TOO_BIG :: _ => emit(JobTooBig)
      case DRAINING :: _ => emit(Draining)
      case OUT_OF_MEMORY :: _ => emit(OutOfMemory)
      case INTERNAL_ERROR :: _ => emit(InternalError)
      case BAD_FORMAT :: _ => emit(BadFormat)
      case UNKNOWN_COMMAND :: _ => emit(UnknownCommand)
      case _ => emit(UnsupportedReply(line))
    }
  }

}
