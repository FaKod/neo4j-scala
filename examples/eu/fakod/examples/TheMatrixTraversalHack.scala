package eu.fakod.examples

import org.neo4j.scala.{EmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}
import sys.ShutdownHookThread
import org.neo4j.graphdb.Traverser.Order
import collection.JavaConversions._
import org.neo4j.graphdb._
import collection.mutable.Buffer

/**
 * The Matrix Example
 * http://wiki.neo4j.org/content/The_Matrix
 */

object TheMatrix2 extends App with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider {

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

  def OUT = Direction.OUTGOING.asInstanceOf[Object]

  def IN = Direction.INCOMING.asInstanceOf[Object]

  def BREADTH_FIRST = Order.BREADTH_FIRST

  implicit def nodeListToTraversal(list: List[Node]) = new {
    def start[T: Manifest](f: (T) => Boolean)(b: Buffer[Object]): List[T] = {
      def args = b.toArray
      val tmp = list.map {
        n =>
          n.traverse(BREADTH_FIRST, StopEvaluator.END_OF_GRAPH,
            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
              case Some(x) => f(x)
              case None => false
            }, args: _*).getAllNodes
      }
      val nodes = (for (c <- tmp; n <- c) yield n).distinct

      (for (n <- nodes; t <- n.toCC[T]) yield t) toList
    }
  }

  def -- = new {
    def apply(s: String) = new {
      val list = Buffer[Object]()

      def --> = {
        list.prepend(OUT)
        list.prepend(DynamicRelationshipType.withName(s).asInstanceOf[Object])
        list
      }

      def --(s: String) = {
        list += DynamicRelationshipType.withName(s).asInstanceOf[Object]
        list += OUT
        this
      }
    }
  }

  withTx {
    implicit neo =>
      val nodeMap = for ((name, prof) <- nodes) yield (name, createNode(Matrix(name, prof)))

      getReferenceNode --> "ROOT" --> nodeMap("Neo")

      nodeMap("Neo") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Neo") --> "KNOWS" --> nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Trinity")
      nodeMap("Morpheus") --> "KNOWS" --> nodeMap("Cypher") --> "KNOWS" --> nodeMap("Agent Smith")
      nodeMap("Agent Smith") --> "CODED_BY" --> nodeMap("The Architect")


      val startNodes = nodeMap("Neo") :: nodeMap("Morpheus") :: nodeMap("Trinity") :: Nil

      val erg = startNodes.start[Matrix](_.name.length > 3)(--("CODED_BY") -- ("KNOWS") -->) sortWith (_.name < _.name)

      val erg2 = startNodes.start[Matrix](_.name.length > 3)(--("KNOWS") -->).foldLeft(0)(_ + _.name.length)

      val erg3 = startNodes.start[Matrix](_.name.length > 3)(--("CODED_BY") -- ("KNOWS") -->).foldLeft("")(_ + _.name)

      println("Relations CODED_BY and KNOWS, sorted by name: " + erg)
      println("Relations KNOWS, length of all names: " + erg2)
      println("Relations CODED_BY and KNOWS, all names appended: " + erg3)
  }

}
