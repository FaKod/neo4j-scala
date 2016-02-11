
organization := "eu.fakod"

name := "neo4j-scala"

version := "0.3.4-SNAPSHOT"

description := "Scala wrapper for Neo4j Graph Database"

crossScalaVersions := Seq("2.11.7", "2.10.5")

homepage := Some(url("http://github.com/fakod/neo4j-scala"))

licenses := Seq(
  "The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)

resolvers += "Neo4j Maven 2 repository" at "https://m2.neo4j.org/content/repositories/releases"

libraryDependencies ++= {

  val baseVersion = if (scalaVersion.value.startsWith("2.11")) "2.3.2" else "2.2.2"

  val neo4jVersion = baseVersion
  val neo4jShellVersion = baseVersion
  val neo4jCypherVersion = baseVersion

  Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,

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

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

fork in Test := true

javaOptions in Test ++= Seq("-Xmx2G")

publishMavenStyle := true

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint", "-unchecked")

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <developers>
    <developer>
      <id>FaKod</id>
      <name>Christopher Schmidt</name>
      <timezone>+1</timezone>
      <email>info [at] FaKod.EU</email>
      <roles>
        <role>developer</role>
      </roles>
    </developer>
  </developers>
    <scm>
      <developerConnection>scm:git:git@github.com:FaKod/neo4j-scala.git</developerConnection>
      <url>git@github.com:FaKod/neo4j-scala.git</url>
    </scm>
  )
