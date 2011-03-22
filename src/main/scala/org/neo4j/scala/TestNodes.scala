package org.neo4j.scala

import org.neo4j.gis.spatial.{EditableLayer, SpatialDatabaseRecord}
import Types._
import annotation.implicitNotFound
import collection.mutable.Buffer

/**
 *
 * @author Christopher Schmidt
 * Date: 22.03.11
 * Time: 06:00
 */

object Types {
  type PointLocation = (Double, Double)
  type PolylineLocation = Buffer[(Double, Double)]
}

trait IsSpatialDatabaseRecord {

  val node: SpatialDatabaseRecord
}

/**
 *
 */
object City {
  val KEY_CITY_NAME = "cityName"

  def apply(node: SpatialDatabaseRecord) = new City(node)

  @implicitNotFound("implicit instance of EditableLayer not in scope")
  def apply(point: PointLocation)(implicit layer: EditableLayer) = {
    val record = layer.add(layer.getGeometryFactory.createPoint(Coord(point._1, point._2)))
    new City(record)
  }
}

/**
 *
 */

import City._

class City(val node: SpatialDatabaseRecord) extends IsSpatialDatabaseRecord {

  def name = node.getProperty(KEY_CITY_NAME)

  def name_=(n: String) {
    node.setProperty(KEY_CITY_NAME, n)
  }
}

object FedaralState {
  val KEY_FEDSTATE_NAME = "federalState"

  def apply(node: SpatialDatabaseRecord) = new FedaralState(node)

  @implicitNotFound("implicit instance of EditableLayer not in scope")
  def apply(shell: PolylineLocation)(implicit layer: EditableLayer) = {
    val record = layer.add(layer.getGeometryFactory.createPolygon(LinRing(shell), null))
    new FedaralState(record)
  }
}

import FedaralState._

class FedaralState(val node: SpatialDatabaseRecord) extends IsSpatialDatabaseRecord {
  def name = node.getProperty(KEY_FEDSTATE_NAME)

  def name_=(n: String) {
    node.setProperty(KEY_FEDSTATE_NAME, n)
  }
}