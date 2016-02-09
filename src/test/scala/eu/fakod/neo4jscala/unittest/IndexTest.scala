package eu.fakod.neo4jscala.unittest

import java.util.UUID

import org.specs2.mutable.SpecificationWithJUnit
import eu.fakod.neo4jscala.{Neo4jIndexProvider, EmbeddedGraphDatabaseServiceProvider, Neo4jWrapper}
import collection.JavaConversions._
import sys.ShutdownHookThread

/**
 * Test spec to check usage of index convenience methods
 *
 * @author Christopher Schmidt
 */

class IndexTestSpec extends SpecificationWithJUnit with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider with Neo4jIndexProvider {

  def neo4jStoreDir = "./target/temp-neo-index-test" + UUID.randomUUID()

  override def NodeIndexConfig = ("MyTestIndex", Map("provider" -> "lucene", "type" -> "fulltext")) :: Nil

  "Neo4jIndexProvider" should {

    ShutdownHookThread {
      shutdown(ds)
    }

    "use the fulltext search index" in {

      withTx {
        implicit db =>

          val nodeIndex = getNodeIndex("MyTestIndex").get

          val theMatrix = createNode()
          val theMatrixReloaded = createNode()
          theMatrixReloaded.setProperty("name", "theMatrixReloaded")

          nodeIndex +=(theMatrix, "title", "The Matrix")
          nodeIndex +=(theMatrixReloaded, "title", "The Matrix Reloaded")

          // search in the fulltext index
          val found = nodeIndex.query("title", "reloAdEd")
          found.size must beGreaterThanOrEqualTo(1)
      }
    }

    "remove items from index" in {


      withTx {
        implicit db =>

          val nodeIndex = getNodeIndex("MyTestIndex").get

          val found = nodeIndex.query("title", "reloAdEd")
          val size = found.size
          for (f <- found.iterator)
            nodeIndex -= f

          // search in the fulltext index
          val found2 = nodeIndex.query("title", "reloAdEd")
          found2.size must beLessThanOrEqualTo(size)
      }
    }
  }
}
