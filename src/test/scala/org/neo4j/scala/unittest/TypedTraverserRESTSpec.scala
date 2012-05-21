package org.neo4j.scala.unittest

import org.specs2.mutable.SpecificationWithJUnit
import java.net.URI
import sys.ShutdownHookThread
import org.neo4j.scala._


class TypedTraverserRESTSpec extends SpecificationWithJUnit with Neo4jWrapper with RestGraphDatabaseServiceProvider with RestTypedTraverser {

  def uri = new URI("http://localhost:7474/db/data/")

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

      /**
       * follow list of nodes with relations BOTH "KNOWS" and OUT "CODED_BY"
       */
      val erg = startNodes.doTraverse[Test_MatrixBase](follow -- "KNOWS" ->- "CODED_BY") {
        /**
         * prune evaluator is Java Script and returns false
         */
        PruneEvaluator("false")
      } {
        /**
         * Return filter (running on client side)
         */
        case (x: Test_Matrix, tp) if (tp.depth == 3) => x.name.length > 2
        case (x: Test_NonMatrix, _) => false

        /**
         * converted to List[Test_MatrixBase]
         */
      }.toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Agent Smith", "Program"), Test_Matrix("The Architect", "Whatever"))
      erg.length must be_==(2)
    }


    "be able to traverse one Node with JS Prune Evaluator" in {
      val erg = nodeMap("Neo").doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY" -<- "FOO") {

        /**
         * using prune eval as Java Script code
         */
        "position.length() > 100;"
      } {
        case (x: Test_Matrix, tp) if (tp.depth == 2) => x.name.length > 2
        case (x: Test_NonMatrix, _) => false
      }.toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Cypher", "Hacker"))
      erg.length must be_==(1)
    }


    "be able to traverse one Node with Max Depth 100" in {

      /**
       * setting max depth to 100 instead of prune evaluator
       */
      val erg = nodeMap("Neo").doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY")(100) {
        case (x: Test_Matrix, tp) if (tp.depth == 2) => x.name.length > 2
      }.toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Cypher", "Hacker"))
      erg.length must be_==(1)
    }


    "be able to traverse with builtin filter \"ReturnAllButStartNode\" with Max Depth 1" in {

      /**
       * set max depth to 1 and using builtin ReturnAllButStartNode function
       */
      val erg = nodeMap("Neo").
        doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY", 1, ReturnAllButStartNode).
        toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Morpheus", "Hacker"), Test_Matrix("Trinity", "Hacker"))
      erg.length must be_==(2)
    }


    "be able to traverse with JS Filter \"true\" with Max Depth 1" in {

      /**
       * set max depth to 1 and using Java Script "true" code
       */
      val erg = nodeMap("Neo").
        doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY", 1, "true").
        toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Neo", "Hacker"), Test_Matrix("Morpheus", "Hacker"), Test_Matrix("Trinity", "Hacker"))
      erg.length must be_==(3)
    }


    "be able to traverse nodes of type Test_Matrix" in {

      /**
       * server side type check with endNode.isOfType[Test_Matrix] that created Java Script code
       */
      val erg = nodeMap("Neo").doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY", 1,
        endNode.isOfType[Test_Matrix]
      ).toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Neo", "Hacker"), Test_Matrix("Morpheus", "Hacker"), Test_Matrix("Trinity", "Hacker"))
      erg.length must be_==(3)
    }

    "be able to traverse nodes of type Test_Matrix and Prune Evaluator" in {

      /**
       * server side type check with endNode.isOfType[Test_Matrix] that created Java Script code
       */
      val erg = nodeMap("Neo").doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY",
        "position.length() >= 1",
        endNode.isOfType[Test_Matrix]
      ).toList.sortWith(_.name < _.name)

      erg must contain(Test_Matrix("Neo", "Hacker"), Test_Matrix("Morpheus", "Hacker"), Test_Matrix("Trinity", "Hacker"))
      erg.length must be_==(3)
    }
  }
}