package org.neo4j.scala.unittest

import org.neo4j.graphdb.Node
import org.specs2.mutable.SpecificationWithJUnit
import org.neo4j.scala.{EmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}
import org.neo4j.scala.util.CaseClassDeserializer

/**
 * Test spec to check deserialization and serialization of case classes
 *
 * @author Christopher Schmidt
 */

case class Test(s: String, i: Int, ji: java.lang.Integer, d: Double, l: Long, b: Boolean)

import CaseClassDeserializer._
class DeSerializingWithoutNeo4jSpec extends SpecificationWithJUnit {

  "De- and Serializing" should {

    "able to create an instance from map" in {
      val m = Map[String, AnyRef]("s" -> "sowas", "i" -> "1", "ji" -> "2", "d" -> (3.3).asInstanceOf[AnyRef], "l" -> "10", "b" -> "true")
      var r = deserialize[Test](m)

      r.s must endWith("sowas")
      r.i must_== (1)
      r.ji must_== (2)
      r.d must_== (3.3)
      r.l must_== (10)
      r.b must_== (true)
    }

    "able to create a map from an instance" in {
      var o = Test("sowas", 1, 2, 3.3, 10, true)
      var resMap = serialize(o)

      resMap.size must_== 6
      resMap.get("d").get mustEqual (3.3)
      resMap.get("b").get mustEqual (true)
    }
  }
}

class DeSerializingSpec extends SpecificationWithJUnit with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider {

  def neo4jStoreDir = "/tmp/temp-neo-test2"

  "Node" should {

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        ds.gds.shutdown
      }
    })

    "be serializable" in {
      var o = Test("sowas", 1, 2, 3.3, 10, true)
      var node: Node = null
      withTx {
        implicit neo =>
          node = createNode(o)
      }

      var oo = Neo4jWrapper.deSerialize[Test](node)
      oo must beEqualTo(o)
    }
  }
}