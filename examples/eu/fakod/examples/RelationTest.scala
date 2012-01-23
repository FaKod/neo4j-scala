package eu.fakod.examples

import collection.mutable.Buffer
import org.neo4j.graphdb.Traverser.Order
import org.neo4j.graphdb.{Direction, DynamicRelationshipType}

/**
 *
 * @author Christopher Schmidt
 * Date: 18.01.12
 * Time: 06:57
 */

object PathVar extends Enumeration {
  type PathVar = Value
  val A, B, C, D, E, F, G, No, x = Value
}

import PathVar._

object Dir extends Enumeration {
  type Dir = Value
  val IN, OUT, NONE = Value
}

import Dir._

class RelationBuffer(firstString: String, pv: PathVar, d: Dir) {

  val pathVarBuf = Buffer[PathVar]()
  val stringBuf = Buffer[String]()
  val dirBuf = Buffer[Dir]()

  if (pv != null) pathVarBuf += pv
  if (firstString != null) stringBuf += firstString
  if (d != null) dirBuf += d

  def this(pv: PathVar) = this (null, pv, null)

  def this(s: String) = this (s, null, null)

  def this(s: String, d: Dir) = this (s, null, d)


  private def store(s: String, d: Dir, pv: PathVar) = {
    stringBuf += s
    dirBuf += d
    if (pv != null) pathVarBuf += pv
  }

  private def store(pv: PathVar, d: Dir) = {
    pathVarBuf += pv
    if (d != null) dirBuf += d
  }

  private def store(s: String, d: Dir) = {
    stringBuf += s
    if (d != null) dirBuf += d
  }

  private def store(s: String, pv: PathVar) = {
    stringBuf += s
    pathVarBuf += pv
  }

  private def store(d: Dir) = dirBuf += d

  private def arrowRight = {
    dirBuf.remove(dirBuf.length - 1)
    dirBuf += OUT
  }

  def --(s: String) = {
    store(s, NONE)
    this
  }

  def --(pv: PathVar) = {
    store(pv, NONE)
    this
  }

  def --() = {
    store(NONE)
    this
  }

  def ->-(s: String) = {
    arrowRight
    store(s, No)
    this
  }

  def ->-(pv: PathVar) = {
    arrowRight
    pathVarBuf += pv
    this
  }

  def ->-() = {
    arrowRight
    pathVarBuf += No
    this
  }

  def -<-(s: String) = {
    store(s, IN)
    this
  }

  def -<-(pv: PathVar) = {
    store(pv, IN)
    this
  }

  def -<-() = {
    store(IN)
    this
  }

  override def toString = ("\npathVarBuf: " + pathVarBuf + " stringBuf: " + stringBuf + " dirBuf: " + dirBuf)
}

trait DoRelation {
  //  def OUT = Direction.OUTGOING.asInstanceOf[Object]
  //
  //  def IN = Direction.INCOMING.asInstanceOf[Object]
  //
  //  def BREADTH_FIRST = Order.BREADTH_FIRST

  implicit def toRelationBuffer(pv: PathVar) = new RelationBuffer(pv)

  def --(s: String) = new RelationBuffer(s, No, NONE)

  def -<-(s: String) = new RelationBuffer(s, No, IN)

}

object RelationTest extends App with DoRelation {

  def aa = A -<- "aaKNOWS" ->- B -- "CODED_BY" ->- C

  def bb = x -- ("bbKNOWS") ->- B -<- "CODED_BY" -- C

  def cc = A -- ("ccKNOWS") ->- x -- ("CODED_BY") ->- C

  def dd = A -- ("ddKNOWS") ->- B -- ("CODED_BY") ->- x

  def ee = --("eeKNOWS") ->- ("CODED_BY") ->-

  def ff = -<-("ffKNOWS") -- ("CODED_BY") ->-

  def gg = --("ggKNOWS") -- ("CODED_BY") --

  def hh = --("hhKNOWS") -<- ("CODED_BY") --


  def ii = A -- "iiKNOWS" ->- E -- "CODED_BY" ->-

  def jj = A -<- ("jjKNOWS") -- E -- "CODED_BY" --

  def kk = C -- "kkKNOWS" -- "CODED_BY" -- "WHAT_EVER" ->-

  def ll = D -- "llKNOWS" -<- "CODED_BY" --


  println(aa)
  println(bb)
  println(cc)
  println(dd)
  println(ee)
  println(ff)
  println(gg)
  println(hh)
  println(ii)
  println(jj)
  println(kk)
  println(ll)
}