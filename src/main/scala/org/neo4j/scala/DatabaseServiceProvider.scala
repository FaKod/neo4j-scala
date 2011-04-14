package org.neo4j.scala

import org.neo4j.kernel.EmbeddedGraphDatabase
import org.neo4j.gis.spatial.SpatialDatabaseService

/**
 * provides an embedded database service
 *
 * @author Christopher Schmidt
 */
trait EmbeddedGraphDatabaseServiceProvider {

  def neo4jStoreDir: String

  val ds: DatabaseService = DatabaseServiceImpl(new EmbeddedGraphDatabase(neo4jStoreDir))
}

/**
 * provides an spatial database service from a given GraphDatabaseService
 */
trait SpatialDatabaseServiceProvider {
  self: EmbeddedGraphDatabaseServiceProvider =>

  val sds = new SpatialDatabaseService(ds.gds)
}