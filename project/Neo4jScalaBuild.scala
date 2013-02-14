import sbt._
import sbt.Keys._

object Neo4jScalaBuild extends Build {
  
  val neo4jVersion = "1.9.M04"
  val neo4jShellVersion = neo4jVersion
  val neo4jCypherVersion = neo4jVersion
  val neo4jRestVersion = neo4jVersion

  lazy val neo4jScala = Project(
    id = "neo4j-scala",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Neo4j Scala",
      organization := "org.neo4j",
      version := "0.2.0-M2-SBT-SNAPSHOT",
      scalaVersion := "2.10.0",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      resolvers += "Neo4j Public Repository" at "http://m2.neo4j.org",
      libraryDependencies ++= Seq("junit" % "junit" % "4.7",
    		  					 "org.specs2" % "specs2_2.10" % "1.13" % "test",
    		  					 "org.specs2" % "scalaz_2.10" % "7.0.0" % "test",
    		  					 "org.neo4j" % "neo4j-kernel" % neo4jVersion,
    		  					 "org.neo4j" % "neo4j-lucene-index" % neo4jVersion,
    		  					 "org.neo4j" % "neo4j-shell" % neo4jShellVersion,
    		  					 "org.neo4j" % "neo4j-rest-graphdb" % neo4jRestVersion,
    		  					 "org.neo4j" % "neo4j-cypher" % neo4jCypherVersion,
    		  					 "org.scala-lang" % "scala-compiler" % "2.10.0")
    )
  )
}
