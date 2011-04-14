package org.neo4j.scala

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.gis.spatial.SpatialDatabaseService

/**
 * Interface for GraphDatabaseService
 *
 * @author Christopher Schmidt
 */
trait DatabaseService {
  def gds: GraphDatabaseService
}

/**
 * standard DatabaseService store for GraphDatabaseService
 */
case class DatabaseServiceImpl(gds: GraphDatabaseService) extends DatabaseService

/**
 * extended store for combined GraphDatabaseService and SpatialDatabaseService
 * used by Neo4jSpatialWrapper
 */
case class CombinedDatabaseService(gds: GraphDatabaseService, sds: SpatialDatabaseService) extends DatabaseService

