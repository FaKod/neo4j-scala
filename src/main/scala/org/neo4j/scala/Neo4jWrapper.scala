package org.neo4j.scala

import org.neo4j.graphdb.{PropertyContainer, RelationshipType, Node}

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
 *
 * Feel free to use this example to tell all your friends how awesome scala is :)
 */
trait Neo4jWrapper extends Neo4jWrapperUtil {

  def ds: DatabaseService

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
   *
   */
  def createNode(implicit ds: DatabaseService): Node = ds.gds.createNode
}

/**
 * creates incoming and outgoing relationships
 */
private[scala] class NodeRelationshipMethods(node: Node) {
  def -->(relType: RelationshipType) = new OutgoingRelationshipBuilder(node, relType)
  def <--(relType: RelationshipType) = new IncomingRelationshipBuilder(node, relType)
}

/**
 * Half-way through building an outgoing relationship
 */
private[scala] class OutgoingRelationshipBuilder(fromNode: Node, relType: RelationshipType) {
  def -->(toNode: Node) = {
    fromNode.createRelationshipTo(toNode, relType)
    new NodeRelationshipMethods(toNode)
  }
}

/**
 * Half-way through building an incoming relationship
 */
private[scala] class IncomingRelationshipBuilder(toNode: Node, relType: RelationshipType) {
  def <--(fromNode: Node) = {
    fromNode.createRelationshipTo(toNode, relType)
    new NodeRelationshipMethods(fromNode)
  }
}

/**
 * convenience for handling properties
 */
private[scala] class RichPropertyContainer(propertyContainer: PropertyContainer) {
  def apply(property: String): Option[Any] =
    propertyContainer.hasProperty(property) match {
      case true => Some(propertyContainer.getProperty(property))
      case _ => None
    }
  def update(property: String, value: Any): Unit = propertyContainer.setProperty(property, value)
}