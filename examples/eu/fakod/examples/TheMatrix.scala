package eu.fakod.examples

import org.neo4j.scala.{EmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}
import sys.ShutdownHookThread
import org.neo4j.graphdb.Traverser.Order
import collection.JavaConversions._
import org.neo4j.graphdb._

/**
 * The Matrix Example
 * http://wiki.neo4j.org/content/The_Matrix
 */
case class Matrix(name: String, profession: String)

object TheMatrix extends App with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider {

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

  withTx {
    implicit neo =>
      val nodeMap = for ((name, prof) <- nodes) yield (name, createNode(Matrix(name, prof)))

      getReferenceNode --> "ROOT" --> nodeMap("Neo")

      nodeMap("Neo") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Neo") --> "KNOWS" --> nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Cypher") --> "KNOWS" --> nodeMap("Agent Smith")
      nodeMap("Agent Smith") --> "CODED_BY" --> nodeMap("The Architect")

      /**
       * Find the friends
       */

      println("\n***** Find the friends")

      nodeMap("Neo").traverse(Order.BREADTH_FIRST,
        StopEvaluator.END_OF_GRAPH,
        ReturnableEvaluator.ALL_BUT_START_NODE,
        DynamicRelationshipType.withName("KNOWS"),
        Direction.OUTGOING).foreach {
        n =>
          n.toCC[Matrix] match {
            case None => println("not a Matrix Case Class")
            case Some(Matrix(name, prof)) => println("Name: " + name + " Profession: " + prof)
          }
      }

      /**
       * Find the hackers
       */

      println("\n***** Find the hackers")

      def isReturnableNode(currentPosition: TraversalPosition) =
        currentPosition.lastRelationshipTraversed match {
          case null => false
          case rel => rel.isType(DynamicRelationshipType.withName("CODED_BY"))
        }

      val traverser = nodeMap("Neo").traverse(Order.BREADTH_FIRST,
        StopEvaluator.END_OF_GRAPH,
        isReturnableNode _,
        DynamicRelationshipType.withName("CODED_BY"),
        Direction.OUTGOING,
        DynamicRelationshipType.withName("KNOWS"),
        Direction.OUTGOING)

        traverser.foreach {
        n =>
          n.toCC[Matrix] match {
            case None => println("not a Matrix Case Class")
            case Some(Matrix(name, prof)) =>
              println("At depth " +traverser.currentPosition.depth + " Name: " + name + " Profession: " + prof)
          }
      }
  }

}
