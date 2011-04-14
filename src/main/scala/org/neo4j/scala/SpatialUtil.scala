package org.neo4j.scala

import org.neo4j.gis.spatial._
import collection.mutable.Buffer
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence
import com.vividsolutions.jts.geom._
import collection.JavaConversions._

/**
 * Really simple and very incomplete list of spatial utility classes
 *
 * @author Christopher Schmidt
 * Date: 14.04.11
 * Time: 06:17
 */

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