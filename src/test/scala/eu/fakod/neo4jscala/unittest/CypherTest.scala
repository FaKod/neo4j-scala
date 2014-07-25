package eu.fakod.neo4jscala.unittest

import org.specs2.mutable.SpecificationWithJUnit
import eu.fakod.neo4jscala._
import sys.ShutdownHookThread
import eu.fakod.neo4jscala.Test_Matrix


class CypherSpec extends SpecificationWithJUnit with Neo4jWrapper with SingletonEmbeddedGraphDatabaseServiceProvider with Cypher {

  def neo4jStoreDir = "/tmp/temp-neo-CypherTest"

  ShutdownHookThread {
    shutdown(ds)
  }

  final val nodes = Map("Neo" -> "Hacker",
    "Morpheus" -> "Hacker",
    "Trinity" -> "Hacker",
    "Cypher" -> "Hacker",
    "Agent Smith" -> "Program",
    "The Architect" -> "Whatever")


  val nodeMap = withTx {
    implicit neo =>
      val nodeMap = for ((name, prof) <- nodes) yield (name, createNode(Test_Matrix(name, prof)))

      nodeMap("Neo") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Neo") --> "KNOWS" --> nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Cypher") --> "KNOWS" --> nodeMap("Agent Smith")
      nodeMap("Agent Smith") --> "CODED_BY" --> nodeMap("The Architect")
      nodeMap
  }

  val startNodes = nodeMap("Neo") :: nodeMap("Morpheus") :: nodeMap("Trinity") :: Nil

  "Cypher Trait" should {

    "be able to execute query" in {
      val query = "start n=node(" + nodeMap("Neo").getId + ") return n, n.name"

      withTx { neo =>
        val typedResult = query.execute.asCC[Test_Matrix]("n")

        typedResult.next.name must be_==("Neo")

        success
      }
    }

    "be able to execute (*) query" in {

      val query = """start n=node(*) where n.name="Neo" return n"""

      withTx { neo =>
        val typedResult = query.execute.asCC[Test_Matrix]("n")
        typedResult.toList.size must be_>(0)
        success
      }
    }

  }
}
