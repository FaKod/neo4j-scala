
organization := "eu.fakod"

name := "neo4j-scala"

version := "0.3.1-SNAPSHOT"

description := "Scala wrapper for Neo4j Graph Database"

crossScalaVersions := Seq("2.11.7", "2.10.4", "2.12.0-M3")

homepage := Some(url("http://github.com/fakod/neo4j-scala"))

licenses := Seq(
  "The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)

resolvers += "Neo4j Maven 2 repository" at "https://m2.neo4j.org/content/repositories/releases"

libraryDependencies ++= {
  val neo4jVersion = "2.1.3"
  val neo4jShellVersion = "2.1.3"
  val neo4jCypherVersion = "2.1.3"
  val neo4jRestVersion = "2.0.1"

  Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,

    // REST Binding -->
    "org.neo4j" % "neo4j-rest-graphdb" % neo4jRestVersion,

    // Neo4j graph database
    "org.neo4j" % "neo4j-kernel" % neo4jVersion,
    "org.neo4j" % "neo4j-lucene-index" % neo4jVersion,
    "org.neo4j" % "neo4j-shell" % neo4jVersion,

    // Cypher
    "org.neo4j" % "neo4j-cypher" % neo4jCypherVersion,

    // Test
    "org.specs2" %% "specs2" % "2.3.13" % "test",
    "junit" % "junit" % "4.11" % "test"
  )
}

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint", "-unchecked")

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <issueManagement>
    <system>github</system>
    <url>https://github.com/FaKod/neo4j-scala/issues</url>
  </issueManagement>
  <developers>
    <developer>
      <id>FaKod</id>
      <name>Christopher Schmidt</name>
      <email>info [at] FaKod.EU</email>
      <timezone>+1</timezone>
      <roles>
        <role>developer</role>
      </roles>
    </developer>
  </developers>
  <scm>
    <url>git@github.com:FaKod/neo4j-scala.git</url>
    <connection>scm:git:git@github.com:FaKod/neo4j-scala.git</connection>
    <developerConnection>scm:git:git@github.com:FaKod/neo4j-scala.git</developerConnection>
  </scm>
)

