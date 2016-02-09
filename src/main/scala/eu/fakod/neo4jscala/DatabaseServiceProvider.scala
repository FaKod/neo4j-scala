package eu.fakod.neo4jscala

import scala.collection.JavaConversions._

import java.net.URI
import java.util.{HashMap => jMap}

import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.unsafe.batchinsert.{BatchInserters, BatchInserter}

/**
 * Interface for a GraphDatabaseServiceProvider
 * must be implemented by and Graph Database Service Provider
 */
trait GraphDatabaseServiceProvider {
  val ds: DatabaseService
}


/**
 * provides a specific Database Service
 * in this case an embedded database service
 */
trait EmbeddedGraphDatabaseServiceProvider extends GraphDatabaseServiceProvider {

  /**
   * directory where to store the data files
   */
  def neo4jStoreDir: String

  /**
   * setup configuration parameters
   * @return Map[String, String] configuration parameters
   */
  def configParams = Map[String, String]()

  /**
   * using an instance of an embedded graph database
   */
  val ds: DatabaseService = {
    DatabaseServiceImpl(
      new GraphDatabaseFactory()
        .newEmbeddedDatabaseBuilder(neo4jStoreDir)
        .setConfig(mapAsJavaMap(configParams))
        .newGraphDatabase
    )
  }
}

/**
 * singleton EmbeddedGraphDatabase
 */
private[neo4jscala] object SingeltonProvider {
  private var ds: Option[DatabaseService] = None

  def apply(neo4jStoreDir: String, configParams: Map[String, String]) = ds match {
    case Some(x) => x
    case None =>
      ds = Some(DatabaseServiceImpl(new GraphDatabaseFactory()
          .newEmbeddedDatabaseBuilder(neo4jStoreDir)
          .setConfig(new jMap[String, String](configParams))
          .newGraphDatabase))
      ds.get
  }
}

/**
 * provides a specific Database Service
 * in this case an singleton embedded database service
 */
trait SingletonEmbeddedGraphDatabaseServiceProvider extends GraphDatabaseServiceProvider {

  /**
   * directory where to store the data files
   */
  def neo4jStoreDir: String

  /**
   * setup configuration parameters
   * @return Map[String, String] configuration parameters
   */
  def configParams = Map[String, String]()

  /**
   * using an instance of an embedded graph database
   */
  val ds: DatabaseService = SingeltonProvider(neo4jStoreDir, configParams)
}

/**
 * singleton provider
 */
private[neo4jscala] object SingeltonBatchProvider {
  private var inserter: Option[BatchInserter] = None

  def apply(neo4jStoreDir: String) = inserter match {
    case Some(x) => x
    case None =>
      inserter = Some(BatchInserters.inserter(neo4jStoreDir))
      inserter.get
  }

  lazy val ds: DatabaseService = DatabaseServiceImpl(new GraphDatabaseFactory()
    .newEmbeddedDatabaseBuilder(inserter.get.getStoreDir)
    .newGraphDatabase)
}

/**
 * provides a specific GraphDatabaseServiceProvider for
 * Batch processing
 */
trait BatchGraphDatabaseServiceProvider extends GraphDatabaseServiceProvider {


  /**
   * instance of BatchInserter
   */
  def batchInserter = SingeltonBatchProvider(neo4jStoreDir)

  /**
   * directory where to store the data files
   */
  def neo4jStoreDir: String

  /**
   * using an instance of an embedded graph database
   */
  val ds: DatabaseService = SingeltonBatchProvider.ds
}
