organization := "com.github.cb372"

name := "finagle-beanstalk"

ideaProjectName := "finagle-beanstalk"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.9.2"

resolvers += "Twitter Maven repo" at "http://maven.twttr.com/"

libraryDependencies ++= Seq(
    // Note: Cannot use latest Finagle (5.3.1)
    // because it pulls in util-core 5.3.1, which breaks naggati :(
    "com.twitter" % "finagle-core" % "5.0.0",
    "com.twitter" % "naggati_2.9.2" % "4.0.0",
    "com.twitter" % "util-logging" % "5.0.0" % "runtime"
    )

libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.4" % "test"
    )

libraryDependencies += "org.scalatest" %% "scalatest" % "1.7.2" % "test"

scalacOptions += "-unchecked"

//publishTo := Some(Resolver.file("file",  new File( "../cb372.github.com/m2/releases" )) )

publishTo <<= version { (v: String) =>
  val local = new File("../cb372.github.com/m2")
  if (v.trim.endsWith("SNAPSHOT"))
    Some(Resolver.file("snapshots", new File(local, "snapshots")))
  else
    Some(Resolver.file("releases", new File(local, "releases")))
}