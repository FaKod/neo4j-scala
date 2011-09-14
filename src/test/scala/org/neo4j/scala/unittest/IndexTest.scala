package org.neo4j.scala.unittest

import org.specs2.mutable.SpecificationWithJUnit
import org.neo4j.scala.{Neo4jIndexProvider, EmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}

/**
 * Test spec to check usage of index convenience methods
 *
 * @author Christopher Schmidt
 */

class IndexTestSpec extends SpecificationWithJUnit with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider with Neo4jIndexProvider {

  def neo4jStoreDir = "/tmp/temp-neo-index-test"

  override def NodeIndexConfig = ("MyTestIndex", Map("provider" -> "lucene", "type" -> "fulltext")) :: Nil


  "Neo4jIndexProvider" should {

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        ds.gds.shutdown
      }
    })

    "use the fulltext search index" in {

      val nodeIndex = getNodeIndex("MyTestIndex").get

      withTx {
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