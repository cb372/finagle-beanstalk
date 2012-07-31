package com.github.cb372.finagle.beanstalk.client

import com.twitter.finagle.builder.ClientBuilder
import com.github.cb372.finagle.beanstalk.protocol._
import com.twitter.naggati.{Codec => NaggatiCodec}
import com.twitter.finagle.{Codec, CodecFactory, Service, ClientCodecConfig}
import com.twitter.util.Future

/**
 * Author: chris
 * Created: 7/29/12
 */

object BeanstalkClient {
  type BeanstalkService = Service[Command, Reply]

  def build(hosts: String) = {
    // TODO more config options

    val service = ClientBuilder()
       .codec(BeanstalkClientCodec())
       .hosts(hosts)
       .hostConnectionLimit(1)
       .retries(2)                         // (1) per-request retries
       .build()

    new BeanstalkClient(service)
  }

}

/**
 * Options for the "put" command
 * @param priority priority of the job (0 - IntMax)
 * @param delay how long to delay the job (seconds, 0 or more)
 * @param timeToRun how long to give a worker to perform the job
 */
case class PutOpts(priority: Int = 0, delay: Int = 0, timeToRun: Int = 1)

import BeanstalkClient.BeanstalkService

class BeanstalkClient(service: BeanstalkService) {

  /**
   * Insert the given string as a beanstalkd job
   * @param data the job
   * @param options configuration options
   * @param charset charset of the string
   * @return reply from the server
   */
  def put(data: String, options: PutOpts)(implicit charset: String = "UTF-8"): Future[Reply] = {
    put(data.getBytes(charset), options)
  }

  /**
   * Insert the given bytes as a beanstalkd job
   * @param data the job
   * @param options configuration options
   * @return reply from the server
   */
  def put(data: Array[Byte], options: PutOpts): Future[Reply] = {
    service(Put(options.priority, options.delay, options.timeToRun, data))
  }

  /**
   * Reserve a job, with no timeout
   * @return
   */
  def reserve(): Future[Reply] = {
    service(Reserve)
  }

  /**
   * Reserve a job, with a timeout
   * @param timeout timeout in seconds
   * @return
   */
  def reserve(timeout: Int): Future[Reply] = {
    service(ReserveWithTimeout(timeout))
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