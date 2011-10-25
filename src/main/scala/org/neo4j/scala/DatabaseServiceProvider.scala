package org.neo4j.scala

import org.neo4j.kernel.EmbeddedGraphDatabase
import org.neo4j.kernel.impl.batchinsert.{BatchInserter, BatchInserterImpl}

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
   * using an instance of an embedded graph database
   */
  val ds: DatabaseService = DatabaseServiceImpl(new EmbeddedGraphDatabase(neo4jStoreDir))
}

/**
 * singleton EmbeddedGraphDatabase
 */
private[scala] object SingeltonProvider {
  private var ds: Option[DatabaseService] = None

  def apply(neo4jStoreDir: String) = ds match {
    case Some(x) => x
    case None =>
      ds = Some(DatabaseServiceImpl(new EmbeddedGraphDatabase(neo4jStoreDir)))
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
   * using an instance of an embedded graph database
   */
  val ds: DatabaseService = SingeltonProvider(neo4jStoreDir)
}

/**
 * singleton provider
 */
private[scala] object SingeltonBatchProvider {
  private var inserter: Option[BatchInserter] = None

  def apply(neo4jStoreDir: String) = inserter match {
    case Some(x) => x
    case None =>
      inserter = Some(new BatchInserterImpl(neo4jStoreDir))
      inserter.get
  }

  lazy val ds: DatabaseService = DatabaseServiceImpl(inserter.get.getGraphDbService)
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