package org.neo4j.scala

import org.neo4j.kernel.EmbeddedGraphDatabase
import org.specs.runner.JUnit4
import org.specs.Specification
import com.vividsolutions.jts.geom.{Coordinate, Envelope}
import org.neo4j.gis.spatial.query.{SearchWithin, SearchContain}
import collection.JavaConversions.asScalaBuffer
import collection.mutable.Buffer
import org.neo4j.gis.spatial.{NullListener, SpatialDatabaseRecord, EditableLayer}
import org.neo4j.graphdb.{Direction, DynamicRelationshipType, GraphDatabaseService}

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

    "blah blah" in {

      withSpatialTx {
        implicit db =>

        deleteLayer("test", new NullListener)
        val layer: EditableLayer = getOrCreateEditableLayer("test")

        val record = layer.add(layer.getGeometryFactory.createPoint(new Coordinate(15.3, 56.2)))
        val searchQuery = new SearchContain(layer.getGeometryFactory().toGeometry(new Envelope(15.0, 16.0, 56.0, 57.0)))
        layer.getIndex.executeSearch(searchQuery)

        var results: Buffer[SpatialDatabaseRecord] = searchQuery.getResults

        results.size must_== 0

        val withinQuery = new SearchWithin(layer.getGeometryFactory().toGeometry(new Envelope(15.0, 16.0, 56.0, 57.0)))
        layer.getIndex().executeSearch(withinQuery)
        results = withinQuery.getResults

        results.size must_== 1
      }

    }
  }
}