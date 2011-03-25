package org.neo4j.scala

import org.specs.runner.JUnit4
import org.specs.Specification
import org.neo4j.gis.spatial.query.{SearchWithin, SearchContain}
import collection.JavaConversions.asScalaBuffer
import collection.mutable.Buffer
import org.neo4j.gis.spatial.{NullListener, SpatialDatabaseRecord}
import java.util.ArrayList
import com.vividsolutions.jts.geom.{LinearRing, Coordinate, Envelope}
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence
import collection.JavaConversions._
import org.neo4j.graphdb.{Node, RelationshipType, Direction, DynamicRelationshipType}

class Neo4jSpatialSpecTest extends JUnit4(Neo4jSpatialSpec)

object Neo4jSpatialSpec extends Specification with Neo4jSpatialWrapper with EmbeddedGraphDatabaseServiceProvider with SpatialDatabaseServiceProvider {

  def neo4jStoreDir = "/tmp/temp-neo-spatial-test"

  "NeoSpatialWrapper" should {
    shareVariables()

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        ds.gds.shutdown
      }
    })

    "allow usage of Neo4jWrapper" in {

      withSpatialTx {
        implicit db =>

        val start = createNode
        val end = createNode
        val relType = DynamicRelationshipType.withName("foo")
        start --> relType --> end
        start.getSingleRelationship(relType, Direction.OUTGOING).
          getOtherNode(start) must beEqualTo(end)
      }
    }

    "simplify layer, node and search usage" in {

      withSpatialTx {
        implicit db =>

        // remove existing layer
        try {
          deleteLayer("test", new NullListener)
        }
        catch {
          case _ =>
        }

        val cities = createNode
        val federalStates = createNode

        withLayer(getOrCreateEditableLayer("test")) {
          implicit layer =>

          // adding Point
          val munich = add newPoint ((15.3, 56.2))
          munich.setProperty("City", "Munich")
          cities --> "isCity" --> munich

          // adding new Polygon
          val bayernBuffer = Buffer[(Double, Double)]((15, 56), (16, 56), (15, 57), (16, 57), (15, 56))
          val bayern = add newPolygon (LinRing(bayernBuffer))
          bayern.setProperty("FederalState", "Bayern")
          federalStates --> "isFederalState" --> bayern

          munich --> "CapitalCityOf" --> bayern

          withSearchWithin(bayern.getGeometry) {
            implicit s =>
              executeSearch
            for (r <- getResults)
              r.getProperty("City") must beEqual("Munich")
          }

          withSearchWithin(toGeometry(new Envelope(15.0, 16.0, 56.0, 57.0))) {
            implicit s =>
              executeSearch
            getResults.size must_== 2
          }
        }
      }
    }
  }
}