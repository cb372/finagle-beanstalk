organization := "com.github.cb372"
name := "finagle-beanstalk"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.7"
resolvers += "Twitter Maven repo" at "http://maven.twttr.com/"

libraryDependencies ++= Seq(
  "com.twitter"    %% "finagle-core"    % "6.31.0",
  "ch.qos.logback"  % "logback-classic" % "1.0.4" % "test",
  "org.scalatest"  %% "scalatest"       % "2.2.4" % "test"
)

scalacOptions += "-unchecked"

publishTo <<= version { (v: String) =>
  val local = new File("../cb372.github.com/m2")
  if (v.trim.endsWith("SNAPSHOT"))
    Some(Resolver.file("snapshots", new File(local, "snapshots")))
  else
    Some(Resolver.file("releases", new File(local, "releases")))
}
