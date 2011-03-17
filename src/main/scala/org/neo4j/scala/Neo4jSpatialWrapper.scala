package org.neo4j.scala

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.gis.spatial.{EditableLayer, Listener, SpatialDatabaseService}

/**
 *
 * @author Christopher Schmidt
 * Date: 16.03.11
 * Time: 16:18
 */
trait Neo4jSpatialWrapper extends Neo4jWrapper {

  def ds: DatabaseService

  val sds: SpatialDatabaseService

  /**
   * Execute instructions within a Neo4j transaction; rollback if exception is raised and
   * commit otherwise; and return the return value from the operation.
   */
  def withSpatialTx[T <: Any](operation: CombinedDatabaseService => T): T = {
    val tx = synchronized {
      ds.gds.beginTx
    }
    try {
      val ret = operation(CombinedDatabaseService(ds.gds, sds))
      tx.success
      return ret
    } finally {
      tx.finish
    }
  }

  def deleteLayer(name: String, monitor: Listener)(implicit db: CombinedDatabaseService) =
    db.sds.deleteLayer(name, monitor)

  def getOrCreateEditableLayer(name: String)(implicit db: CombinedDatabaseService): EditableLayer =
    db.sds.getOrCreateEditableLayer(name)

  /**
   * methods from Neo4jWrapper usage should still be possible
   */
  implicit def databaseServiceToGraphDatabaseService(ds:CombinedDatabaseService):GraphDatabaseService = ds.gds
}