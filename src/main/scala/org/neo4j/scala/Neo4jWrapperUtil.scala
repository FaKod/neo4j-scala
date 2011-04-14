package org.neo4j.scala

import org.neo4j.graphdb._

/**
 * trait for implicits
 * used by Neo4j wrapper
 *
 * @author Christopher Schmidt
 */
trait Neo4jWrapperUtil {

  implicit def node2relationshipBuilder(node: Node) = new NodeRelationshipMethods(node)

  implicit def string2RelationshipType(relType: String) = DynamicRelationshipType.withName(relType)

  implicit def propertyContainer2RichPropertyContainer(propertyContainer: PropertyContainer) = new RichPropertyContainer(propertyContainer)

  implicit def fn2StopEvaluator(e: TraversalPosition => Boolean) =
    new StopEvaluator() {
      def isStopNode(traversalPosition: TraversalPosition) = e(traversalPosition)
    }

  implicit def fn2ReturnableEvaluator(e: TraversalPosition => Boolean) =
    new ReturnableEvaluator() {
      def isReturnableNode(traversalPosition: TraversalPosition) = e(traversalPosition)
    }

  /**
   * Stuff for Indexes
   */

  implicit def indexManager(implicit ds: DatabaseService) = ds.gds.index
}
