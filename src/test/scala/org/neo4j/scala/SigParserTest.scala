package org.neo4j.scala

import org.specs.runner.JUnit4
import org.specs.Specification
import util.{CaseClassDeserializer, JavaType}
import CaseClassDeserializer._

/**
 *
 * @author Christopher Schmidt
 * Date: 20.07.11
 * Time: 06:29
 */

case class Test(s: String, i: Int, ji: java.lang.Integer, d: Double, l: Long, b: Boolean)


class DeSerializingTest extends JUnit4(DeSerializingSpec)

object DeSerializingSpec extends Specification {

  "DeSerializing" should {

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

      resMap.size must_==  6
      resMap.get("d").get mustEqual (3.3)
      resMap.get("b").get mustEqual (true)
    }
  }
}