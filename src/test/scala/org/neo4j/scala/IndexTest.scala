package org.neo4j.scala

import org.specs.Specification
import org.specs.runner.JUnit4
import collection.JavaConversions._

/**
 *
 * @author Christopher Schmidt
 * Date: 12.04.11
 * Time: 06:15
 */

class IndexTest extends JUnit4(IndexTestSpec)

object IndexTestSpec extends Specification with Neo4jSpatialWrapper with EmbeddedGraphDatabaseServiceProvider
with SpatialDatabaseServiceProvider with Neo4jIndexProvider {

  def neo4jStoreDir = "/tmp/temp-neo-index-test"

  override def NodeIndexConfig = ("MyTestIndex", Map("provider" -> "lucene", "type" -> "fulltext")) :: Nil


  "Neo4jIndexProvider" should {
    shareVariables()

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        ds.gds.shutdown
      }
    })

    "use the fulltext search index" in {

      val nodeIndex = getNodeIndex("MyTestIndex").get

      withSpatialTx {
        implicit db =>

        val theMatrix = createNode
        val theMatrixReloaded = createNode
        theMatrixReloaded.setProperty("name", "theMatrixReloaded")

        nodeIndex += (theMatrix, "title", "The Matrix")
        nodeIndex += (theMatrixReloaded, "title", "The Matrix Reloaded")
        
        // search in the fulltext index
        val found = nodeIndex.query("title", "reloAdEd")
        found.size must beGreaterThanOrEqualTo(1)
      }
    }
  }
}