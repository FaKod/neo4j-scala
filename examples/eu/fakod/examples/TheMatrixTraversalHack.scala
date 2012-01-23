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

case class Tp[T](dao: T, tp: TraversalPosition)


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


  /**
   *
   */
  implicit def nodeListToTraversal(list: List[Node]) = new {

    //    def startTp2[T: Manifest](b: Buffer[Object])(stopEval: (T, TraversalPosition) => Boolean)(retEval: (T, TraversalPosition) => Boolean)(f: (List[T]) => Unit): Unit = {
    //      def args = b.toArray
    //
    //      val tmp = list.par.map {
    //        n =>
    //          n.traverse(BREADTH_FIRST,
    //            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
    //              case Some(x) => stopEval(x, tp)
    //              case None => false
    //            },
    //            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
    //              case Some(x) => retEval(x, tp)
    //              case None => false
    //            }, args: _*).getAllNodes
    //      }
    //
    //      val nodes = (for (c <- tmp; n <- c) yield n).distinct
    //
    //      val ergList = (for (n <- nodes; t <- n.toCC[T]) yield t).toList
    //      f(ergList)
    //    }

    def startTp2[T: Manifest](b: Buffer[Object])(stopEval: PartialFunction[(T, TraversalPosition), Boolean])(retEval: PartialFunction[(T, TraversalPosition), Boolean])(f: (List[T]) => Unit): Unit = {
      def args = b.toArray

      val tmp = list.par.map {
        n =>
          n.traverse(BREADTH_FIRST,
            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
              case Some(x) if (stopEval.isDefinedAt(x, tp)) => stopEval(x, tp)
              case _ => false
            },
            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
              case Some(x) if (retEval.isDefinedAt(x, tp)) => retEval(x, tp)
              case _ => false
            }, args: _*).getAllNodes
      }

      val nodes = (for (c <- tmp; n <- c) yield n).distinct

      val ergList = (for (n <- nodes; t <- n.toCC[T]) yield t).toList
      f(ergList)
    }

    /**
     *
     */
    def startTp[T: Manifest](stopEval: (Tp[T]) => Boolean)(retEval: (Tp[T]) => Boolean)(b: Buffer[Object]): List[T] = {
      def args = b.toArray
      val tmp = list.map {
        n =>
          n.traverse(BREADTH_FIRST,
            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
              case Some(x) => stopEval(Tp(x, tp))
              case None => false
            },
            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
              case Some(x) => retEval(Tp(x, tp))
              case None => false
            }, args: _*).getAllNodes
      }

      val nodes = (for (c <- tmp; n <- c) yield n).distinct

      (for (n <- nodes; t <- n.toCC[T]) yield t).toList
    }

    /**
     *
     */
    def start[T: Manifest](retEval: (T) => Boolean)(b: Buffer[Object]): List[T] = {
      def args = b.toArray
      val tmp = list.par.map {
        n =>
          n.traverse(BREADTH_FIRST, StopEvaluator.END_OF_GRAPH,
            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
              case Some(x) => retEval(x)
              case None => false
            }, args: _*).getAllNodes
      }

      val nodes = (for (c <- tmp; n <- c) yield n).distinct

      (for (n <- nodes; t <- n.toCC[T]) yield t).toList
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

  val erg = startNodes.start[Matrix](_.name.length > 3)(--("CODED_BY") -- ("KNOWS") -->) sortWith (_.name < _.name)

  val erg2 = startNodes.start[Matrix](_.name.length > 3)(--("KNOWS") -->).foldLeft(0)(_ + _.name.length)

  val erg3 = startNodes.start[Matrix](_.name.length > 3)(--("CODED_BY") -- ("KNOWS") -->).foldLeft("")(_ + _.name)

  println("Relations CODED_BY and KNOWS, sorted by name: " + erg)
  println("Relations KNOWS, length of all names: " + erg2)
  println("Relations CODED_BY and KNOWS, all names appended: " + erg3)


  /**
   * the following does not seem to be very convenient ;-) for FP newbies
   * the question is: How do we get type safety through the stop and return evaluator?
   * In case of the return evaluator its clear that we want to return only instances of Matrix
   * the stop evaluator should work with all kinds of nodes - right? So this implementation is "wrong"
   *
   * maybe its better to define Tp like this
   *    case class Tp(dao: AnyRef, tp: TraversalPosition)
   * and to create the dao dynamically depending on the nodes "__CLASS__" property
   *
   * The stop evaluator could be written f. e. like
   *
   * {
   *   case Matrix(name, _) => name.equalsIgnoreCase("Stop Here") // stop if name is "Stop Here"
   *   case s:Something => true                                   // stop if node is of type Something
   *   case _ => false                                            // default: don't stop
   * }
   */
  val ergTp = startNodes.startTp[Matrix] {
    case Tp(x, tp) => false
  } {
    case Tp(x, tp) => x.name.length > 3 && tp.depth == 2
  }(--("CODED_BY") -- ("KNOWS") -->) sortWith (_.name < _.name)

  println("Relations CODED_BY and KNOWS, sorted by name and depth == 2: " + ergTp)


  val startWithNodes = nodeMap("Neo") :: nodeMap("Morpheus") :: nodeMap("Trinity") :: Nil


  startWithNodes.startTp2[MatrixBase](--("KNOWS") -- ("CODED_BY") -->) {
    case _ => false
  } {
    case (x: Matrix, tp) if (tp.depth > 0) => x.name.length > 2
    case (x: NonMatrix, _) => false
    case _ => false
  } {
    result =>
      val erg = result.sortWith(_.name < _.name)
      println("Relations CODED_BY and KNOWS, sorted by name and depth == 2: " + erg)
  }


  startWithNodes.startTp2[MatrixBase](--("KNOWS") -- ("CODED_BY") -->) {
    case _ => false
  } {
    case (x: Matrix, tp) if (tp.depth > 0) => x.name.length > 2
    case (x: NonMatrix, _) => false
    case _ => false
  } {
    result =>
      val erg = result.sortWith(_.name < _.name)
      println("Relations CODED_BY and KNOWS, sorted by name and depth == 2: " + erg)
  }


}
