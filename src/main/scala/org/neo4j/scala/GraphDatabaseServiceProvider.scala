package org.neo4j.scala

import org.neo4j.kernel.EmbeddedGraphDatabase
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.gis.spatial.SpatialDatabaseService

/**
 *
 * @author Christopher Schmidt
 * Date: 16.03.11
 * Time: 16:27
 */

/**
 * Interface to access the GraphDatabaseService
 */
trait DatabaseService {
  def gds: GraphDatabaseService
}

/**
 * normal DatabaseService store for GraphDatabaseService
 */
case class DatabaseServiceImpl(gds: GraphDatabaseService) extends DatabaseService

/**
 * extended store for combined GraphDatabaseService and SpatialDatabaseService
 * used by Neo4jSpatialWrapper
 */
case class CombinedDatabaseService(gds: GraphDatabaseService, sds: SpatialDatabaseService) extends DatabaseService


/**
 * provides an embedded database service
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