package org.neo4j.scala

import util.CaseClassDeserializer
import collection.JavaConversions._
import org.neo4j.graphdb.{Relationship, PropertyContainer, RelationshipType, Node}
import CaseClassDeserializer._

/**
 * Extend your class with this trait to get really neat new notation for creating
 * new relationships. For example, ugly Java-esque code like:
 * <pre>
 * val knows = DynamicRelationshipType.withName("KNOWS")
 * start.createRelationshipTo(intermediary, knows)
 * intermediary.createRelationshipTo(end, knows)
 * </pre>
 *
 * can be replaced with a beautiful Scala one-liner:
 * <pre>start --> "KNOWS" --> intermediary --> "KNOWS" --> end</pre>
 */
trait Neo4jWrapper extends GraphDatabaseServiceProvider with Neo4jWrapperImplicits {

  /**
   * Execute instructions within a Neo4j transaction; rollback if exception is raised and
   * commit otherwise; and return the return value from the operation.
   */
  def withTx[T <: Any](operation: DatabaseService => T): T = {
    val tx = synchronized {
      ds.gds.beginTx
    }
    try {
      val ret = operation(ds)
      tx.success
      return ret
    } finally {
      tx.finish
    }
  }

  /**
   * creates a new Node from Database service
   */
  def createNode(implicit ds: DatabaseService): Node = ds.gds.createNode

  /**
   * convenience method to create and serialize a case class
   */
  def createNode[T <: Product](cc: T)(implicit ds: DatabaseService): Node = Neo4jWrapper.serialize(cc, createNode)
}

/**
 * Neo4jWrapper Object
 */
object Neo4jWrapper extends Neo4jWrapperImplicits {
  /**
   * this name will be used to store the class name of
   * the serialized case class that will be verified
   * in deserialization
   */
  val ClassPropertyName = "__CLASS__"
  /**
   * serializes a given case class into a Node instance
   */
  def serialize[T <: Product](cc: T, node: Node): Node = {
    CaseClassDeserializer.serialize(cc).foreach {
      case (name, value) => node.setProperty(name, value)
    }
    node(ClassPropertyName) = cc.getClass.getName
    node
  }

  /**
   * deserializes a given case class type from a given Node instance
   * throws a IllegalArgumentException if a Nodes properties
   * do not fit to the case class properties
   */
  def deSerialize[T <: Product](node: Node)(implicit m: ClassManifest[T]): T = {
    node[String](ClassPropertyName) match {
      case Some(cpn) =>
        val kv = for (k <- node.getPropertyKeys; v = node.getProperty(k)) yield (k -> v)
        val o = deserialize[T](kv.toMap)(m)
        if (!cpn.equalsIgnoreCase(o.getClass.getName))
          throw new IllegalArgumentException("given Case Class does not fit to serialized properties")
        o
      case _ =>
        throw new IllegalArgumentException("this is not a Node with a serialized case class")
    }
  }
}

/**
 * creates incoming and outgoing relationships
 */
private[scala] class NodeRelationshipMethods(node: Node, rel: Relationship = null) {
  def -->(relType: RelationshipType) = new OutgoingRelationshipBuilder(node, relType)

  def <--(relType: RelationshipType) = new IncomingRelationshipBuilder(node, relType)

  /**
   * use this to get the created relationship object
   * <pre>start --> "KNOWS" --> end <;</pre>
   */
  def < = rel
}

/**
 * Half-way through building an outgoing relationship
 */
private[scala] class OutgoingRelationshipBuilder(fromNode: Node, relType: RelationshipType) {
  def -->(toNode: Node) = {
    val rel = fromNode.createRelationshipTo(toNode, relType)
    new NodeRelationshipMethods(toNode, rel)
  }
}

/**
 * Half-way through building an incoming relationship
 */
private[scala] class IncomingRelationshipBuilder(toNode: Node, relType: RelationshipType) {
  def <--(fromNode: Node) = {
    val rel = fromNode.createRelationshipTo(toNode, relType)
    new NodeRelationshipMethods(fromNode, rel)
  }
}

/**
 * convenience for handling properties
 */
private[scala] class RichPropertyContainer(propertyContainer: PropertyContainer) {

  /**
   * type of properties is normally Object
   * use type identifier T to cast it
   */
  def apply[T](property: String): Option[T] =
    propertyContainer.hasProperty(property) match {
      case true => Some(propertyContainer.getProperty(property).asInstanceOf[T])
      case _ => None
    }

  /**
   * updates the property
   * <code>node("property") = value</code>
   */
  def update(property: String, value: Any): Unit =
    propertyContainer.setProperty(property, value)
}