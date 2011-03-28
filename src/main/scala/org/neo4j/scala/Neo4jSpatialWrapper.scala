package org.neo4j.scala

import org.neo4j.gis.spatial._
import collection.mutable.Buffer
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence
import com.vividsolutions.jts.geom._
import org.neo4j.graphdb.{Node, GraphDatabaseService}
import query.SearchWithin
import collection.JavaConversions._

/**
 *
 * @author Christopher Schmidt
 * Date: 16.03.11
 * Time: 16:18
 */


trait IsSpatialDatabaseRecord {
  val node: SpatialDatabaseRecord
}


trait Neo4jSpatialWrapper extends Neo4jWrapper with Neo4jSpatialWrapperImplicits {

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

/**
 *
 */
trait Neo4jSpatialWrapperImplicits {

  /**
   * Search convenience defs
   */

  def withSearchWithin[T <: Any](geometry: Geometry)(operation: (SearchWithin) => T): T = {
    val search = new SearchWithin(geometry)
    operation(search)
  }

//  def searchWithin(geometry: Geometry)(implicit layer: EditableLayer) = {
//    val search = new SearchWithin(geometry)
//    layer.getIndex.executeSearch(search)
//    val result: Buffer[SpatialDatabaseRecord] = search.getResults
//    result
//  }

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

private[scala] class AddGeometry(layer: EditableLayer) {
  val gf = layer.getGeometryFactory

  def newPoint(coordinate: Coordinate): SpatialDatabaseRecord = layer.add(gf.createPoint(coordinate))

  def newPolygon(shell: LinearRing, holes: Array[LinearRing] = null) = layer.add(gf.createPolygon(shell, holes))
}


/**
 * convenience object of handling Coordinates
 */
object Coord {
  def apply(x: Double, y: Double) = new Coordinate(x, y)
}

/**
 * convenience object for CoordinateArraySequence
 */
object CoordArraySequence {
  def apply(b: Buffer[(Double, Double)]) = {
    val a = for (t <- b; c = Coord(t._1, t._2)) yield c
    new CoordinateArraySequence(a.toArray)
  }
}

/**
 * convenience object for LinearRing
 */
object LinRing {
  def apply(cs: CoordinateSequence)(implicit layer: EditableLayer) =
    new LinearRing(cs, layer.getGeometryFactory)

  def apply(b: Buffer[(Double, Double)])(implicit layer: EditableLayer) =
    new LinearRing(CoordArraySequence(b), layer.getGeometryFactory)
}