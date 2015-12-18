package com.github.cb372.finagle.beanstalk.protocol

import com.github.cb372.finagle.beanstalk.naggati.Encoder
import com.github.cb372.finagle.beanstalk.naggati.codec.BeanstalkCodec
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

case class Delete(id: Int) extends Command {
  // SPEC: are negative IDs allowed?

  def toChannelBuffer = {
    val tokens = List(Commands.DELETE, id)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class Release(id: Int, priority: Int, delay: Int) extends Command {
  // SPEC: are negative IDs allowed?
  require(priority >= 0, "priority must not be negative")
  require(delay >= 0, "delay must not be negative")

  def toChannelBuffer = {
    val tokens = List(Commands.RELEASE, id, priority, delay)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class Bury(id: Int, priority: Int) extends Command {
  // SPEC: are negative IDs allowed?
  require(priority >= 0, "priority must not be negative")

  def toChannelBuffer = {
    val tokens = List(Commands.BURY, id, priority)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class Touch(id: Int) extends Command {
  // SPEC: are negative IDs allowed?

  def toChannelBuffer = {
    val tokens = List(Commands.TOUCH, id)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class Watch(tube: String) extends Command {
  import Validation.isValidName
  require(isValidName(tube), "invalid tube name")

  def toChannelBuffer = {
    val tokens = List(Commands.WATCH, tube)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class Ignore(tube: String) extends Command {
  import Validation.isValidName
  require(isValidName(tube), "invalid tube name")

  def toChannelBuffer = {
    val tokens = List(Commands.IGNORE, tube)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class Peek(id: Int) extends Command {
  // SPEC: are negative IDs allowed?

  def toChannelBuffer = {
    val tokens = List(Commands.PEEK, id)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object PeekReady extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.PEEK_READY)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object PeekDelayed extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.PEEK_DELAYED)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object PeekBuried extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.PEEK_BURIED)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class Kick(max: Int) extends Command {
  require(max >= 0, "upper bound must not be negative")

  def toChannelBuffer = {
    val tokens = List(Commands.KICK, max)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class StatsJob(id: Int) extends Command {
  // SPEC: are negative IDs allowed?

  def toChannelBuffer = {
    val tokens = List(Commands.STATS_JOB, id)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class StatsTube(tube: String) extends Command {
  import Validation.isValidName
  require(isValidName(tube), "invalid tube name")

  def toChannelBuffer = {
    val tokens = List(Commands.STATS_TUBE, tube)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object Stats extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.STATS)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object ListTubes extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.LIST_TUBES)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object ListTubeUsed extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.LIST_TUBE_USED)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object ListTubesWatched extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.LIST_TUBES_WATCHED)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case object Quit extends Command {
  def toChannelBuffer = {
    val tokens = List(Commands.QUIT)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

case class PauseTube(tube: String, delay: Int) extends Command {
  import Validation.isValidName
  require(isValidName(tube), "invalid tube name")
  require(delay >= 0, "delay must not be negative")

  def toChannelBuffer = {
    val tokens = List(Commands.PAUSE_TUBE, tube, delay)
    BeanstalkCodec.oneLineToChannelBuffer(tokens)
  }
}

object Commands {
  val PUT = "put"
  val USE = "use"
  val RESERVE = "reserve"
  val RESERVE_WITH_TIMEOUT = "reserve-with-timeout"
  val DELETE = "delete"
  val RELEASE = "release"
  val BURY = "bury"
  val TOUCH = "touch"
  val WATCH = "watch"
  val IGNORE = "ignore"
  val PEEK = "peek"
  val PEEK_READY = "peek-ready"
  val PEEK_DELAYED = "peek-delayed"
  val PEEK_BURIED = "peek-buried"
  val KICK = "kick"
  val STATS_JOB = "stats-job"
  val STATS_TUBE = "stats-tube"
  val STATS = "stats"
  val LIST_TUBES = "list-tubes"
  val LIST_TUBE_USED = "list-tube-used"
  val LIST_TUBES_WATCHED = "list-tubes-watched"
  val QUIT = "quit"
  val PAUSE_TUBE = "pause-tube"

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