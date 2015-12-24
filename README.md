# finagle-beanstalk

[![Build Status](https://travis-ci.org/cb372/finagle-beanstalk.svg?branch=master)](https://travis-ci.org/cb372/finagle-beanstalk)

An asynchronous Scala client for [beanstalkd](http://kr.github.io/beanstalkd/) built with [Finagle](https://twitter.github.io/finagle/).

## Versioning

Built for Scala 2.11.x.

## How to use

Add the following dependency to your sbt build file:

```
libraryDependencies += "com.github.cb372" %% "finagle-beanstalk" % "0.0.1"
```

In your Scala application, create a new `BeanstalkClient` by passing it a comma-separated list of `host:port` pairs:

```scala
import com.github.cb372.finagle.beanstalk.client.BeanstalkClient

val client = BeanstalkClient.build("host1:11300,host2:11300")
```

Then use it, e.g.:

```scala
val jobData = "foo"
val putOptions = PutOpts(priority = 1, delay = 2, timeToRun = 3)
val futureOfResponse: Future[Either[Reply, Int]] = client.put(jobData, putOptions)
futureOfResponse map {
  case Left(reply) => 
    // Server replied with an error.
    // Pattern match on it to find out what it was.
  case Right(id) =>
    // The job was successfully inserted.
    // This is the job ID.
}
```

Finally shut down the client:

```scala
client.quit()
```

## How to run the tests

Make sure you have `beanstalkd` installed and available on your `$PATH`. The tests will automatically start a `beanstalkd` server on a random port.

Then run:

```
$ sbt test
```
