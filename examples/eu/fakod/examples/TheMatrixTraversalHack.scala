package eu.fakod.examples

import sys.ShutdownHookThread
import org.neo4j.scala.{TypedTraverser, SingletonEmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}


trait MatrixBase {
  val name: String
  val profession: String
}

case class Matrix(name: String, profession: String) extends MatrixBase

case class NonMatrix(name: String, profession: String) extends MatrixBase

/**
 * The Matrix Example
 * http://wiki.neo4j.org/content/The_Matrix
 */
object TheMatrix extends App with Neo4jWrapper with SingletonEmbeddedGraphDatabaseServiceProvider with TypedTraverser {

  ShutdownHookThread {
    shutdown(ds)
  }

  def neo4jStoreDir = "/tmp/temp-neo-TheMatrix"

  /**
   * defining nodes
   */
  final val nodes = Map("Neo" -> "Hacker",
    "Morpheus" -> "Hacker",
    "Trinity" -> "Hacker",
    "Cypher" -> "Hacker",
    "Agent Smith" -> "Program",
    "The Architect" -> "Whatever")


  /**
   * creating nodes and associations
   */
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

  // list of Nodes of type: List[Node]
  val startNodes = nodeMap("Neo") :: nodeMap("Morpheus") :: nodeMap("Trinity") :: Nil

  /**
   * traverse starting with all Nodes in startNodes, returning:
   * - direction and type (Incoming "KNOWS")
   * - all Nodes assignable to type MatrixBase
   * - all of type Matrix and depth==1 and MatrixBase.name.length > 2
   * - none of type NonMatrix
   *
   * The resulting List is sorted by name
   *
   */
  val erg1 = startNodes.doTraverse[MatrixBase](follow -<- "KNOWS") {
    case _ => false
  } {
    case (x: Matrix, tp) if (tp.depth == 1) => x.name.length > 2
    case (x: NonMatrix, _) => false
  }.toList.sortWith(_.name < _.name)

  println("Relations KNOWS, sorted by name and depth == 1: " + erg1)

  /**
   * traverse starting with Node "Neo", returning:
   * - direction and type (Both "KNOWS" and Outgoing "CODED_BY")
   * - all Nodes assignable to type MatrixBase
   * - all of type Matrix and depth==2 and MatrixBase.name.length > 2
   * - none of type NonMatrix
   *
   * The resulting List is sorted by name
   *
   */
  val erg2 = nodeMap("Neo").doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY") {
    END_OF_GRAPH
  } {
    case (x: Matrix, tp) if (tp.depth == 2) => x.name.length > 2
    case (x: NonMatrix, _) => false
  }.toList.sortWith(_.name < _.name)

  println("Relations CODED_BY and KNOWS, sorted by name and depth == 2: " + erg2)
}
