package org.neo4j.scala


import org.neo4j.gis.spatial._
import collection.mutable.Buffer
import com.vividsolutions.jts.geom._
import org.neo4j.graphdb.{Node, GraphDatabaseService}
import collection.JavaConversions._
import query.{SearchWithinDistance, SearchWithin}

/**
 * Util and implicits Trait for spatial stuff
 * extended by spatial wrapper
 *
 * @author Christopher Schmidt
 * Date: 14.04.11
 * Time: 06:15
 */
trait Neo4jSpatialWrapperUtil {

  /**
   * Search convenience defs
   */

  def withSearchWithin[T <: Any](geometry: Geometry)(operation: (SearchWithin) => T): T = {
    val search = new SearchWithin(geometry)
    operation(search)
  }

  def searchWithinDistance(point: Point, distance:Double)(implicit layer: EditableLayer) = {
      val search = new SearchWithinDistance(point, distance)
      layer.getIndex.executeSearch(search)
      val result: Buffer[SpatialDatabaseRecord] = search.getResults
      result
    }

  /**
   * handles most of the searches with one Geometry parameter
   */
  def search[T <: AbstractSearch](geometry: Geometry)(implicit layer: EditableLayer, m: ClassManifest[T]) = {
    val ctor = m.erasure.getConstructor(classOf[Geometry])
    val search = ctor.newInstance(geometry).asInstanceOf[T]
    layer.getIndex.executeSearch(search)
    val result: Buffer[SpatialDatabaseRecord] = search.getResults
    result
  }

  def executeSearch(implicit search: SearchWithin, layer: EditableLayer) = layer.getIndex.executeSearch(search)

  def getResults(implicit search: SearchWithin) = search.getResults

  /**
   * node convenience defs
   */

  implicit def IsSpatialDatabaseRecordToNode(r: IsSpatialDatabaseRecord): Node = r.node.getGeomNode

  implicit def record2relationshipBuilder(record: IsSpatialDatabaseRecord) = new NodeRelationshipMethods(record.node)

  /**
   * Database Record convenience defs
   */

  // converts SpatialDatabaseRecord to Node
  implicit def spatialDatabaseRecordToNode(sdr: SpatialDatabaseRecord): Node = sdr.getGeomNode

  // delegation to Neo4jWrapper
  implicit def node2relationshipBuilder(sdr: SpatialDatabaseRecord) = new NodeRelationshipMethods(sdr.getGeomNode)

  implicit def nodeToSpatialDatabaseRecord(node: Node)(implicit layer: Layer): SpatialDatabaseRecord =
    new SpatialDatabaseRecord(layer, node)

  /**
   * DatabaseService Wrapper
   */

  def deleteLayer(name: String, monitor: Listener)(implicit db: CombinedDatabaseService) =
    db.sds.deleteLayer(name, monitor)

  def getOrCreateEditableLayer(name: String)(implicit db: CombinedDatabaseService): EditableLayer =
    db.sds.getOrCreateEditableLayer(name)

  /**
   * methods from Neo4jWrapper usage should still be possible
   */
  implicit def databaseServiceToGraphDatabaseService(ds: CombinedDatabaseService): GraphDatabaseService = ds.gds

  /**
   * Layer Wrapper
   */

  implicit def tupleToCoordinate(t: (Double, Double)): Coordinate = Coord(t._1, t._2)

  def getGeometryFactory(implicit layer: EditableLayer) = layer.getGeometryFactory

  def toGeometry(envelope: Envelope)(implicit layer: EditableLayer): Geometry = getGeometryFactory.toGeometry(envelope)

  //def executeSearch(search: Search)(implicit layer: EditableLayer): Unit = layer.getIndex.executeSearch(search)

  def add(implicit layer: EditableLayer) = new AddGeometry(layer)

}

/**
 * container trait to hold an instance of SpatialDatabaseRecord
 */
trait IsSpatialDatabaseRecord {
  val node: SpatialDatabaseRecord
}