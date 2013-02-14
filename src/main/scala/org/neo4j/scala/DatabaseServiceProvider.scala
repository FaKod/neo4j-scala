package org.neo4j.scala

import org.neo4j.kernel.EmbeddedGraphDatabase
import org.neo4j.unsafe.batchinsert.{BatchInserter, BatchInserterImpl}
import org.neo4j.rest.graphdb.RestGraphDatabase
import java.net.URI
import java.util.{HashMap => jMap}
import org.neo4j.unsafe.batchinsert.BatchInserters

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
    import collection.JavaConversions.mapAsJavaMap
    DatabaseServiceImpl(
      new EmbeddedGraphDatabase(neo4jStoreDir, new jMap[String, String](configParams))
    )
  }
}

/**
 * singleton EmbeddedGraphDatabase
 */
private[scala] object SingeltonProvider {
  private var ds: Option[DatabaseService] = None

  def apply(neo4jStoreDir: String, configParams: Map[String, String]) = ds match {
    case Some(x) => x
    case None =>
      import collection.JavaConversions.mapAsJavaMap
      ds = Some(DatabaseServiceImpl(new EmbeddedGraphDatabase(
        neo4jStoreDir, new jMap[String, String](configParams)))
      )
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
private[scala] object SingeltonBatchProvider {
  private var inserter: Option[BatchInserter] = None

  def apply(neo4jStoreDir: String) = inserter match {
    case Some(x) => x
    case None =>
      inserter = Some(BatchInserters.inserter(neo4jStoreDir))
      inserter.get
  }

  lazy val ds: DatabaseService = DatabaseServiceImpl(BatchInserters.batchDatabase(inserter.get.getStoreDir))
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

/**
 * The Java binding for the Neo4j Server REST API wraps the REST calls
 * behind the well known GraphDatabaseService API
 */
trait RestGraphDatabaseServiceProvider extends GraphDatabaseServiceProvider {

  /**
   * has to be overwritten to define the server location
   * @return URI server URI
   */
  def uri: URI

  /**1
   * has to be overwritten to define username and password
   * @return Option[(String, String)] user and password as Option of Strings
   */
  def userPw: Option[(String, String)] = None

  /**
   * creates a new instance of a REST Graph Database Service
   */
  val ds: DatabaseService = DatabaseServiceImpl(userPw match {
    case None => new RestGraphDatabase(uri.toString)
    case Some((u, p)) => new RestGraphDatabase(uri.toString, u, p)
  })
}