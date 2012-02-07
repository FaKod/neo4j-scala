package eu.fakod.examples

import sys.ShutdownHookThread
import org.neo4j.scala.{TypedTraverser, SingletonEmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}

/**
 * The Matrix Example
 * http://wiki.neo4j.org/content/The_Matrix
 */
object TheMatrix2 extends App with Neo4jWrapper with SingletonEmbeddedGraphDatabaseServiceProvider with TypedTraverser {

  ShutdownHookThread {
    shutdown(ds)
  }

  def neo4jStoreDir = "/tmp/temp-neo-TheMatrix"

  final val nodes = Map("Neo" -> "Hacker",
    "Morpheus" -> "Hacker",
    "Trinity" -> "Hacker",
    "Cypher" -> "Hacker",
    "Agent Smith" -> "Program",
    "The Architect" -> "Whatever")


  val nodeMap = withTx {
    implicit neo =>
      val nodeMap = for ((name, prof) <- nodes) yield (name, createNode(Matrix(name, prof)))

      getReferenceNode --> "ROOT" --> nodeMap("Neo")

      nodeMap("Neo") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Neo") --> "KNOWS" --> nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Cypher") --> "KNOWS" --> nodeMap("Agent Smith")
      nodeMap("Agent Smith") --> "CODED_BY" --> nodeMap("The Architect")
      nodeMap
  }

  val startNodes = nodeMap("Neo") :: nodeMap("Morpheus") :: nodeMap("Trinity") :: Nil

  val erg1 = startNodes.doTraverse[MatrixBase](follow -- "KNOWS" ->- "CODED_BY") {
    case _ => false
  } {
    case (x: Matrix, tp) if (tp.depth == 3) => x.name.length > 2
    case (x: NonMatrix, _) => false
  }.toList.sortWith(_.name < _.name)

  println("Relations CODED_BY and KNOWS, sorted by name and depth == 3: " + erg1)


  val erg2 = nodeMap("Neo").doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY" -<- "FOO") {
    END_OF_GRAPH
  } {
    case (x: Matrix, tp) if (tp.depth == 2) => x.name.length > 2
    case (x: NonMatrix, _) => false
  }.toList.sortWith(_.name < _.name)

  println("Relations CODED_BY and KNOWS, sorted by name and depth == 2: " + erg2)


}
