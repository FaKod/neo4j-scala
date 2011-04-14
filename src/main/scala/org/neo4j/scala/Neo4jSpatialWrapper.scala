package org.neo4j.scala

import org.neo4j.gis.spatial.{EditableLayer, SpatialDatabaseService}

/**
 * Basic Neo4j Spatial Wrapper trait
 *
 * @author Christopher Schmidt
 * Date: 16.03.11
 * Time: 16:18
 */
trait Neo4jSpatialWrapper extends Neo4jWrapper with Neo4jSpatialWrapperUtil {

  implicit val ds: DatabaseService

  implicit val sds: SpatialDatabaseService

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

  /**
   * retrieves the layer object and executes operation
   */
  def withLayer[T <: Any](getLayer: => EditableLayer)(operation: EditableLayer => T): T = {
    val layer = getLayer
    operation(layer)
  }
}



