package org.neo4j.scala

import Types._
import annotation.implicitNotFound
import collection.mutable.Buffer
import org.neo4j.graphdb.{DynamicRelationshipType, Direction}
import org.neo4j.gis.spatial.{SpatialDatabaseService, EditableLayer, SpatialDatabaseRecord}

/**
 * Examples following this Design Guide: http://wiki.neo4j.org/content/Design_Guide
 * 
 * @author Christopher Schmidt
 * Date: 22.03.11
 * Time: 06:00
 */

/**
 * defines some shorter types for this examples
 */
object Types {
  type PointLocation = (Double, Double)
  type PolylineLocation = Buffer[(Double, Double)]
}

/**
 * example implementation for a City Node
 */
class City(val node: SpatialDatabaseRecord) extends IsSpatialDatabaseRecord {
  object City {
    val KEY_CITY_NAME = "cityName"
  }
  def name = node.getProperty(City.KEY_CITY_NAME)
  def name_=(n: String) {
    node.setProperty(City.KEY_CITY_NAME, n)
  }
}

/**
 * example implementation for a polyline node (a federal state)
 */
class FedaralState(val node: SpatialDatabaseRecord) extends IsSpatialDatabaseRecord {
  object FedaralState {
    val KEY_FEDSTATE_NAME = "federalState"
  }
  def name = node.getProperty(FedaralState.KEY_FEDSTATE_NAME)
  def name_=(n: String) {
    node.setProperty(FedaralState.KEY_FEDSTATE_NAME, n)
  }

  def getCapitalCity(implicit layer:EditableLayer) = {
    val n = node.getGeomNode
    val o = n.getSingleRelationship(
      DynamicRelationshipType.withName("CapitalCityOf"), Direction.INCOMING
    ).getOtherNode(node.getGeomNode)
    new City(new SpatialDatabaseRecord(layer, o))
  }
}

/**
 * factory object
 * creates new SpatialDatabaseRecords resp. Nodes via reflection
 */
object NewSpatialNode {

  /**
   * uses a given node to create a instance of IsSpatialDatabaseRecord
   */
  def apply[T: ClassManifest](node: SpatialDatabaseRecord): T = {
    val m = implicitly[ClassManifest[T]]
    val ctor = m.erasure.getConstructor(classOf[SpatialDatabaseRecord])
    ctor.newInstance(node).asInstanceOf[T]
  }

  /**
   * creates a new SpatialDatabaseRecord from a given Geometry
   * and calls the contructor
   */
  @implicitNotFound("implicit instance of EditableLayer not in scope")
  def apply[T](shell: PolylineLocation)(implicit layer: EditableLayer, m: ClassManifest[T]): T = {
    val record = layer.add(layer.getGeometryFactory.createPolygon(LinRing(shell), null))
    apply[T](record)
  }

  /**
   * creates a new SpatialDatabaseRecord from a given Geometry
   * and calls the contructor
   */
  @implicitNotFound("implicit instance of EditableLayer not in scope")
  def apply[T](point: PointLocation)(implicit layer: EditableLayer, m: ClassManifest[T]): T = {
    val record = layer.add(layer.getGeometryFactory.createPoint(Coord(point._1, point._2)))
    apply[T](record)
  }
}