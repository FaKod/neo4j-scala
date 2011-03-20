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

  "NeoWrapper" should {
    shareVariables()

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        ds.gds.shutdown
      }
    })

    "Wrapper usage should be possible" in {

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

    "interim test all" in {

      withSpatialTx {
        implicit db =>

        val cities = createNode

        deleteLayer("test", new NullListener)
        val layer = getOrCreateEditableLayer("test")
        val gf = layer.getGeometryFactory



        withLayer(getOrCreateEditableLayer("test")) {
          implicit layer =>

          // adding Point
          val munich = add newPoint ((15.3, 56.2))
          munich.setProperty("City", "Munich")
          cities --> "isCity" --> munich

          // adding new Polygon
          val buf = Buffer((15.3, 56.2), (15.4, 56.3), (15.5, 56.4), (15.6, 56.5), (15.3, 56.2))
          val bayern = add newPolygon (LinRing(buf))

          bayern.setProperty("FederalState", "Bayern")
          munich --> "CapitalCityOf" --> bayern

          val relation = munich.getSingleRelationship("CapitalCityOf", Direction.OUTGOING)

          relation.getStartNode must beEqual(spatialDatabaseRecordToNode(munich))
          relation.getEndNode must beEqual(spatialDatabaseRecordToNode(bayern))

          for (r <- bayern.getRelationships)
            println(r.getType)

          for (r <- munich.getRelationships)
            println(r.getType)
        }

        // search
        val searchQuery = new SearchContain(layer.getGeometryFactory().toGeometry(new Envelope(15.0, 16.0, 56.0, 57.0)))
        layer.getIndex.executeSearch(searchQuery)

        var results: Buffer[SpatialDatabaseRecord] = searchQuery.getResults

        results.size must_== 0

        val withinQuery = new SearchWithin(layer.getGeometryFactory().toGeometry(new Envelope(15.0, 16.0, 56.0, 57.0)))
        layer.getIndex().executeSearch(withinQuery)
        results = withinQuery.getResults

        results.size must_== 2
      }

    }
  }
}