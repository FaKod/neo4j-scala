package org.neo4j.scala

import util.CaseClassDeserializer
import collection.JavaConversions._
import CaseClassDeserializer._
import org.neo4j.graphdb._
import index.IndexManager

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
  def createNode(cc: AnyRef)(implicit ds: DatabaseService): Node =
    Neo4jWrapper.serialize(cc, createNode)

  /**
   * Looks up a node by id.
   *
   * @param id the id of the node
   * @return the node with id <code>id</code> if found
   * @throws NotFoundException if not found
   */
  def getNodeById(id: Long)(implicit ds: DatabaseService): Node =
    ds.gds.getNodeById(id)

  /**
   * Looks up a relationship by id.
   *
   * @param id the id of the relationship
   * @return the relationship with id <code>id</code> if found
   * @throws NotFoundException if not found
   */
  def getRelationshipById(id: Long)(implicit ds: DatabaseService): Relationship =
    ds.gds.getRelationshipById(id)

  /**
   * Returns the reference node, which is a "starting point" in the node
   * space. Usually, a client attaches relationships to this node that leads
   * into various parts of the node space. For more information about common
   * node space organizational patterns, see the design guide at <a
   * href="http://wiki.neo4j.org/content/Design_Guide"
   * >wiki.neo4j.org/content/Design_Guide</a>.
   *
   * @return the reference node
   * @throws NotFoundException if unable to get the reference node
   */
  def getReferenceNode(implicit ds: DatabaseService): Node =
    ds.gds.getReferenceNode

  /**
   * Returns all nodes in the node space.
   *
   * @return all nodes in the node space
   */
  def getAllNodes(implicit ds: DatabaseService): Iterable[Node] =
    ds.gds.getAllNodes

  /**
   * Returns all relationship types currently in the underlying store.
   * Relationship types are added to the underlying store the first time they
   * are used in a successfully commited {@link Node#createRelationshipTo
   * node.createRelationshipTo(...)}. Note that this method is guaranteed to
   * return all known relationship types, but it does not guarantee that it
   * won't return <i>more</i> than that (e.g. it can return "historic"
   * relationship types that no longer have any relationships in the node
   * space).
   *
   * @return all relationship types in the underlying store
   */
  def getRelationshipTypes(implicit ds: DatabaseService): Iterable[RelationshipType] =
    ds.gds.getRelationshipTypes

  /**
   * Shuts down Neo4j. After this method has been invoked, it's invalid to
   * invoke any methods in the Neo4j API and all references to this instance
   * of GraphDatabaseService should be discarded.
   */
  def shutdown(implicit ds: DatabaseService): Unit =
    ds.gds.shutdown
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
   * for null values not property will be set
   */
  def serialize[T <: PropertyContainer](cc: AnyRef, pc: PropertyContainer): T = {
    CaseClassDeserializer.serialize(cc).foreach {
      case (name, null) =>
      case (name, value) => pc.setProperty(name, value)
    }
    pc(ClassPropertyName) = cc.getClass.getName
    pc.asInstanceOf[T]
  }

  /**
   * conditional case class deserialization
   * Some(T) if possible
   * None if not
   */
  def toCC[T: Manifest](pc: PropertyContainer): Option[T] =
    if (toCCPossible(pc)) {
      val kv = for (k <- pc.getPropertyKeys; v = pc.getProperty(k)) yield (k -> v)
      val o = deserialize[T](kv.toMap)
      Some(o)
    }
    else
      None

  /**
   * only checks if this property container has been serialized
   * with T
   */
  def toCCPossible[T: Manifest](pc: PropertyContainer): Boolean =
    pc[String](ClassPropertyName) match {
      case Some(cpn) if (cpn.equals(manifest[T].erasure.getName)) =>
        true
      case _ =>
        false
    }

  /**
   * deserializes a given case class type from a given Node instance
   * throws a IllegalArgumentException if a Nodes properties
   * do not fit to the case class properties
   */
  def deSerialize[T: Manifest](pc: PropertyContainer): T = {
    toCC[T](pc) match {
      case Some(t) => t
      case _ => throw new IllegalArgumentException("given Case Class: " +
        manifest[T].erasure.getName + " does not fit to serialized properties")
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
   * <pre>start --> "KNOWS" --> end <()</pre>
   */
  def <() = rel

  /**
   * <pre>start --> "KNOWS" --> end <(MyCaseClass(...))</pre>
   */
  def <(cc: AnyRef): Relationship = Neo4jWrapper.serialize(cc, rel)
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