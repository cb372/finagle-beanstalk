organization := "com.github.cb372"
name := "finagle-beanstalk"
scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.twitter"    %% "finagle-core"    % "6.31.0",
  "ch.qos.logback"  % "logback-classic" % "1.0.4" % "test",
  "org.scalatest"  %% "scalatest"       % "2.2.4" % "test"
)

scalacOptions += "-unchecked"

scalariformSettings

// Maven Central stuff
pomExtra :=
  <url>https://github.com/cb372/finagle-beanstalk</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.html</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:cb372/finagle-beanstalk.git</url>
    <connection>scm:git:git@github.com:cb372/finagle-beanstalk.git</connection>
  </scm>
  <developers>
    <developer>
      <id>cb372</id>
      <name>Chris Birchall</name>
      <url>https://github.com/cb372</url>
    </developer>
  </developers>
publishTo <<= version { v =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := { _ => false }

releasePublishArtifactsAction := PgpKeys.publishSigned.value
