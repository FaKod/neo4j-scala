package org.neo4j.scala.unittest

import org.specs2.mutable.SpecificationWithJUnit
import sys.ShutdownHookThread
import org.neo4j.scala._

class TypedTraverserSpec extends SpecificationWithJUnit with Neo4jWrapper with SingletonEmbeddedGraphDatabaseServiceProvider with TypedTraverser {

  def neo4jStoreDir = "/tmp/temp-neo-TypedTraverserSpec"

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

      getReferenceNode --> "ROOT" --> nodeMap("Neo")

      nodeMap("Neo") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Neo") --> "KNOWS" --> nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Cypher") --> "KNOWS" --> nodeMap("Agent Smith")
      nodeMap("Agent Smith") --> "CODED_BY" --> nodeMap("The Architect")
      nodeMap
  }

  val startNodes = nodeMap("Neo") :: nodeMap("Morpheus") :: nodeMap("Trinity") :: Nil

  "TypedTraverser" should {

    "be able to traverse a List of Nodes" in {
      val erg = startNodes.doTraverse[Test_MatrixBase](follow -- "KNOWS" ->- "CODED_BY") {
        case _ => false
      } {
        case (x: Test_Matrix, tp) if (tp.depth == 3) => x.name.length > 2
        case (x: Test_NonMatrix, _) => false
      }.toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Cypher", "Hacker"), Test_Matrix("The Architect", "Whatever"))
      erg.length must be_==(2)
    }

    "be able to traverse one Node" in {
      val erg = nodeMap("Neo").doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY" -<- "FOO") {
        END_OF_GRAPH
      } {
        case (x: Test_Matrix, tp) if (tp.depth == 2) => x.name.length > 2
        case (x: Test_NonMatrix, _) => false
      }.toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Cypher", "Hacker"))
      erg.length must be_==(1)
    }
  }
}