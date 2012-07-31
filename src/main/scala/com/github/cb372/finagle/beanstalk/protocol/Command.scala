package com.github.cb372.finagle.beanstalk.protocol

import com.twitter.naggati.Encoder
import org.jboss.netty.buffer.ChannelBuffer

/**
 * Author: chris
 * Created: 7/27/12
 */

sealed trait Command {
  def toChannelBuffer: ChannelBuffer
  def toByteArray: Array[Byte] = toChannelBuffer.array
}

case class Put(priority: Int, delay: Int, timeToRun: Int, data: Array[Byte]) extends Command {
  require(priority >= 0, "priority must not be negative")
  require(delay >= 0, "delay must not be negative")
  require(timeToRun >= 0, "timeToRun must not be negative")

  def toChannelBuffer = {
    val tokens = List(Commands.PUT, priority, delay, timeToRun, data.length)
    BeanstalkCodec.oneLinePlusDataToChannelBuffer(tokens, data)
  }
}

case class Use(tube: String) extends Command {
  import Validation.isValidName
  require(isValidName(tube), "invalid tube name")

  def toChannelBuffer = {
    val tokens = List(Commands.USE, tube)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object Reserve extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.RESERVE)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class ReserveWithTimeout(timeout: Int) extends Command {
  require(timeout >= 0, "timeout must not be negative")

  def toChannelBuffer = {
    val tokens = List(Commands.RESERVE_WITH_TIMEOUT, timeout)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

object Commands {
  val PUT = "put"
  val USE = "use"
  val RESERVE = "reserve"
  val RESERVE_WITH_TIMEOUT = "reserve-with-timeout"
}

object Validation {
  val validPattern = """[A-Za-z0-9-\+/;\.\$_\(\)]+""".r.pattern
  def isValidName(tube: String): Boolean = {
    tube.length > 0 &&
      tube.length <= 200 &&
      tube.charAt(0) != '-' &&
      validPattern.matcher(tube).matches()
  }
}

object CommandEncoder {

  val encode = new Encoder[Command] {
    def encode(obj: Command) = Some(obj.toChannelBuffer)
  }

}