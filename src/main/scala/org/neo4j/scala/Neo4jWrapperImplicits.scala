package org.neo4j.scala

import org.neo4j.graphdb._

/**
 * trait for implicits
 * used by Neo4j wrapper
 *
 * @author Christopher Schmidt
 */
trait Neo4jWrapperImplicits {

  /**
   * converts to a relationship builder to use --> <-- methods
   */
  implicit def node2relationshipBuilder(node: Node) = new NodeRelationshipMethods(node)

  /**
   * converts a String to a relationship type
   */
  implicit def string2RelationshipType(relType: String) = DynamicRelationshipType.withName(relType)

  /**
   * conversion to use property set and get convenience
   */
  implicit def propertyContainer2RichPropertyContainer(propertyContainer: PropertyContainer) = new RichPropertyContainer(propertyContainer)

  /**
   * creates a functional correct StopEvaluator instance
   */
  implicit def fn2StopEvaluator(e: TraversalPosition => Boolean) =
    new StopEvaluator() {
      def isStopNode(traversalPosition: TraversalPosition) = e(traversalPosition)
    }

  /**
   * creates a functional correct ReturnableEvaluator instance
   */
  implicit def fn2ReturnableEvaluator(e: TraversalPosition => Boolean) =
    new ReturnableEvaluator() {
      def isReturnableNode(traversalPosition: TraversalPosition) = e(traversalPosition)
    }

  /**
   * Stuff for Indexes
   */
  implicit def indexManager(implicit ds: DatabaseService) = ds.gds.index

  /**
   * for serialization
   */
  implicit def nodeToCaseClass(pc: PropertyContainer) = new {
    def toCC[T: Manifest]: Option[T] = Neo4jWrapper.toCC[T](pc)

    def toCCPossible[T: Manifest]: Boolean = Neo4jWrapper.toCCPossible[T](pc)
  }
}
