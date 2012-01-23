package eu.fakod.examples

import org.neo4j.graphdb.Traverser.Order
import collection.mutable.Buffer
import org.neo4j.graphdb._
import org.neo4j.scala.{EmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}

/**
 *
 * @author Christopher Schmidt
 * Date: 22.11.11
 * Time: 07:25
 */

object QueryVariable extends Enumeration {
  val a, b, c, d, e, f, g = Value
}

trait Match {
  def OUT = Direction.OUTGOING.asInstanceOf[Object]

  def IN = Direction.INCOMING.asInstanceOf[Object]

  def BREADTH_FIRST = Order.BREADTH_FIRST

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


}

import QueryVariable._

//object MatchTest extends App with Match {
//  //with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider {
//
//  val t1 = --("CODED_BY") -- ("KNOWS") -->
//
//  println(t1)
//
//  val t2 = start(a = node) {
//    MATCH(a -- ("CODED_BY") -- b -- ("KNOWS") --> c)
//    RETURN(b[Matrix])
//  } {
//    s =>
//  }
//}