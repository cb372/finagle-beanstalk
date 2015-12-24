package com.github.cb372.finagle.beanstalk.client

import com.github.cb372.finagle.beanstalk.naggati.codec.BeanstalkCodec
import com.github.cb372.finagle.beanstalk.naggati.{ Codec => NaggatiCodec }
import com.twitter.finagle.builder.ClientBuilder
import com.github.cb372.finagle.beanstalk.protocol._
import com.twitter.finagle.{ Codec, CodecFactory, Service, ClientCodecConfig }
import com.twitter.util.Future
import com.twitter.util
import yaml.{ StoopidYamlParser, YamlParser }

object BeanstalkClient {
  type BeanstalkService = Service[Command, Reply]

  def build(hosts: String) = {
    // TODO more config options

    val service = ClientBuilder()
      .codec(BeanstalkClientCodec())
      .hosts(hosts)
      .hostConnectionLimit(1)
      .retries(2) // (1) per-request retries
      .build()

    new BeanstalkClient(service)
  }

}

/**
 * Options for the "put" command
 *
 * @param priority priority of the job (0 - IntMax)
 * @param delay how long to delay the job (seconds, 0 or more)
 * @param timeToRun how long to give a worker to perform the job
 */
case class PutOpts(priority: Int = 0, delay: Int = 0, timeToRun: Int = 1)

// TODO support typed jobs and (de)serialization, instead of just raw byte arrays

/**
 * A beanstalk job
 *
 * @param id the job's ID
 * @param data the job's data
 * @tparam A the type of the job's data
 */
case class Job[A](id: Int, data: A)

import BeanstalkClient.BeanstalkService

class BeanstalkClient(service: BeanstalkService,
    yamlParser: YamlParser = new StoopidYamlParser) {

  /**
   * Use the given tube for producing subsequent messages
   *
   * @param tube the tube where every subsequent put will put jobs into
   * @return the tube's name
   */
  def use(tube: String): Future[Either[Reply, String]] = {
    service(Use(tube)).map {
      case Using(tubeName) => Right(tubeName)
      case other => Left(other)
    }
  }

  /**
   * Insert the given string as a beanstalkd job
   *
   * @param data the job
   * @param options configuration options
   * @param charset charset of the string
   * @return the job's ID
   */
  def put(data: String, options: PutOpts)(implicit charset: String = "UTF-8"): Future[Either[Reply, Int]] = {
    put(data.getBytes(charset), options)
  }

  /**
   * Insert the given bytes as a beanstalkd job
   *
   * @param data the job
   * @param options configuration options
   * @return the job's ID, or an error reply
   */
  def put(data: Array[Byte], options: PutOpts): Future[Either[Reply, Int]] =
    service(Put(options.priority, options.delay, options.timeToRun, data)).map {
      case Inserted(id) => Right(id)
      case other => Left(other)
    }

  /**
   * Reserve a job, with no timeout
   *
   * @return a job, or an error reply
   */
  def reserve(): Future[Either[Reply, Job[Array[Byte]]]] =
    service(Reserve).map {
      case Reserved(id, data) => Right(Job[Array[Byte]](id, data))
      case other => Left(other)
    }

  /**
   * Reserve a job, with a timeout
   *
   * @param timeout timeout in seconds
   * @return a job, or an error reply
   */
  def reserve(timeout: Int): Future[Either[Reply, Job[Array[Byte]]]] =
    service(ReserveWithTimeout(timeout)).map {
      case Reserved(id, data) => Right(Job[Array[Byte]](id, data))
      case other => Left(other)
    }

  /**
   * Delete the job with the given ID
   *
   * @param id the job's ID
   * @return true if the job was deleted
   */
  def delete(id: Int): Future[Boolean] =
    service(Delete(id)).map {
      case Deleted => true
      case _ => false
    }

  /**
   * Release the job with the given ID
   *
   * @param id the job's ID
   * @param priority the new priority to give the job
   * @param delay delay in seconds
   * @return true if the job was successfully released
   */
  def release(id: Int, priority: Int, delay: Int): Future[Boolean] =
    service(Release(id, priority, delay)).map {
      case Released => true
      case _ => false
    }

  /**
   * Bury the job with the given ID
   *
   * @param id the job's ID
   * @param priority the new priority to give the job
   * @return true if the job was successfully buried
   */
  def bury(id: Int, priority: Int): Future[Boolean] =
    service(Bury(id, priority)).map {
      case Buried => true
      case _ => false
    }

  /**
   * Touch the job with the given ID, in order to request more time to work
   *
   * @param id the job's ID
   * @return true if the job was successfully found and touched
   */
  def touch(id: Int): Future[Boolean] =
    service(Touch(id)).map {
      case Touched => true
      case _ => false
    }

  /**
   * Start watching the given tube
   *
   * @param tube the tube name to watch
   * @return the number of tubes now being watched, or an error reply
   */
  def watch(tube: String): Future[Either[Reply, Int]] =
    service(Watch(tube)).map {
      case Watching(count) => Right(count)
      case other => Left(other)
    }

  /**
   * Peek at the given job
   *
   * @param id the job ID
   * @return a job, if it exists
   */
  def peek(id: Int): Future[Option[Job[Array[Byte]]]] =
    service(Peek(id)).map { reply => handlePeekReply(reply) }

  /**
   * Peek at the next ready job
   *
   * @return a job, if it exists
   */
  def peekReady(): Future[Option[Job[Array[Byte]]]] =
    service(PeekReady).map { reply => handlePeekReply(reply) }

  /**
   * Peek at the delayed job with the least remaining delay
   *
   * @return a job, if it exists
   */
  def peekDelayed(): Future[Option[Job[Array[Byte]]]] =
    service(PeekDelayed).map { reply => handlePeekReply(reply) }

  /**
   * Peek at the next job in the list of buried jobs
   *
   * @return a job, if it exists
   */
  def peekBuried(): Future[Option[Job[Array[Byte]]]] =
    service(PeekBuried).map { reply => handlePeekReply(reply) }

  /**
   * Kick any buried and/or delayed jobs in the current tube into the ready queue
   *
   * @param max the maximum number of jobs to kick
   * @return the number of jobs actually kicked, or an error reply
   */
  def kick(max: Int): Future[Either[Reply, Int]] =
    service(Kick(max)).map {
      case Kicked(count) => Right(count)
      case other => Left(other)
    }

  /**
   * Retrieve stats about the given job
   *
   * @param id job ID
   * @return stats data
   */
  def statsJob(id: Int): Future[Either[Reply, Map[String, String]]] =
    service(StatsJob(id)).map { reply => handleStatsReply(reply) }

  /**
   * Retrieve stats about the given tube
   *
   * @param tube tube name
   * @return stats data
   */
  def statsTube(tube: String): Future[Either[Reply, Map[String, String]]] =
    service(StatsTube(tube)).map { reply => handleStatsReply(reply) }

  /**
   * Retrieve stats about the beanstalkd server
   *
   * @return stats data
   */
  def stats(): Future[Either[Reply, Map[String, String]]] =
    service(Stats).map { reply => handleStatsReply(reply) }

  /**
   * Retrieve a list of all tubes that currently exist.
   * Note that tubes are created on-demand and disappear
   * when they are not needed anymore (i.e. when they are empty
   * and no clients are watching them).
   *
   * @return
   */
  def listTubes(): Future[Either[Reply, Seq[String]]] =
    service(ListTubes).map { reply => handleYamlListReply(reply) }

  /**
   * Get the name of the tube we are currently using
   *
   * @return
   */
  def listTubeUsed(): Future[Either[Reply, String]] =
    service(ListTubeUsed).map {
      case Using(tube) => Right(tube)
      case other => Left(other)
    }

  /**
   * Retrieve a list of all tubes that we are watching.
   *
   * @return
   */
  def listTubesWatched(): Future[Either[Reply, Seq[String]]] =
    service(ListTubesWatched).map { reply => handleYamlListReply(reply) }

  /**
   * Retrieve a list of all tubes that we are watching.
   *
   * @return
   */
  def quit(): Unit = {
    // Do not bother sending the optional "quit" command, just close the connection
    service.close()
  }

  /**
   * Pause the given tube for the given number of seconds.
   *
   * @return true if the tube was paused, false if it was not found, error otherwise
   */
  def pauseTube(tube: String, delay: Int): Future[Either[Reply, Boolean]] =
    service(PauseTube(tube, delay)).map {
      case Paused => Right(true)
      case NotFound => Right(false)
      case other => Left(other)
    }

  private def handlePeekReply(reply: Reply): Option[Job[Array[Byte]]] =
    reply match {
      case Found(id, data) => Some(Job[Array[Byte]](id, data))
      case other => None
    }

  private def handleStatsReply(reply: Reply): Either[Reply, Map[String, String]] =
    reply match {
      case Ok(data) => Right(yamlParser.parseMap(new String(data, BeanstalkCodec.CHARSET)))
      case other => Left(other)
    }

  private def handleYamlListReply(reply: Reply): Either[Reply, Seq[String]] =
    reply match {
      case Ok(data) => Right(yamlParser.parseList(new String(data, BeanstalkCodec.CHARSET)))
      case other => Left(other)
    }
}

object BeanstalkClientCodec {
  def apply() = new BeanstalkClientCodec
  def get() = apply()
}

class BeanstalkClientCodec extends CodecFactory[Command, Reply]#Client {
  def apply(config: ClientCodecConfig) =
    new Codec[Command, Reply] {
      def pipelineFactory = new NaggatiCodec(ReplyDecoder.decode, CommandEncoder.encode).pipelineFactory
    }

}
