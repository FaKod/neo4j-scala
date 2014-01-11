package eu.fakod.neo4jscala

import scala.language.implicitConversions
import org.neo4j.graphdb.Traverser.Order
import collection.mutable.Buffer
import org.neo4j.graphdb._
import scala.Some

/**
 * Iterator convenience. Mapps a Property Container Iterator to a T Iterator
 * Nodes that can't be converted to T are returned as null
 */
class TypedPropertyContainerIterator[T: Manifest](pcIter: Iterator[PropertyContainer]) extends Iterable[T] with Neo4jWrapperImplicits {
  def iterator = new Iterator[T] {
    def hasNext = pcIter.hasNext

    def next: T = pcIter.next.toCC[T] match {
      case Some(x) => x
      case None => null.asInstanceOf[T]
    }
  }
}

/**
 * Basics for Typed Traverser
 */
trait TypedTraverserBase {

  self: Neo4jWrapper =>

  /**
   * the follow keyword
   * @param order defines the Order of traversal
   */
  def follow(order: Order) = new RelationBuffer(order)

  /**
   * the follow keyword
   * traversal defaults to DEPTH_FIRST
   */
  def follow: RelationBuffer = follow(Order.DEPTH_FIRST)

  /**
   * Container to store the Direction and Relationtype
   * @param order defines the Order of traversal
   */
  final class RelationBuffer(val order: Order) {

    import DynamicRelationshipType._
    import Direction._

    private val list = Buffer[Object]()

    private def store(d: Direction, s: String) = {
      list += withName(s)
      list += d
      this
    }

    /**
     * Incoming direction
     * @param s String type of relation
     */
    def -<-(s: String) = store(INCOMING, s)

    /**
     * Both directions
     * @param s String type of relation
     */
    def --(s: String) = store(BOTH, s)

    /**
     * Outgoing direction
     * @param s String type of relation
     */
    def ->-(s: String) = store(OUTGOING, s)

    private[neo4jscala] def get = list.toArray

    private[neo4jscala] def getOrder = order
  }

  /**
   * shortens Order.BREADTH_FIRST
   */
  final val BREADTH_FIRST = Order.BREADTH_FIRST

  /**
   * shortens Order.DEPTH_FIRST
   */
  final val DEPTH_FIRST = Order.DEPTH_FIRST
}


/**
 * Trait for a Typed Traversal API
 */
trait TypedTraverser extends TypedTraverserBase {
  self: Neo4jWrapper =>

  /**
   * END_OF_GRAPH Stop Evaluator
   * stops at end of graph only
   */
  final def END_OF_GRAPH[T]: PartialFunction[(T, TraversalPosition), Boolean] = {
    case _ => false
  }

  /**
   * ALL Return Evaluator
   * Returns all Nodes
   */
  final def ALL[T]: PartialFunction[(T, TraversalPosition), Boolean] = {
    case _ => true
  }

  /**
   * ALL_BUT_START_NODE Return Evaluator
   * returns all Nodes except start node
   */
  final def ALL_BUT_START_NODE[T]: PartialFunction[(T, TraversalPosition), Boolean] = {
    case (_, tp) => tp.notStartNode
  }


  /**
   * Enhances a Node with a doTraverse method
   * @param node start node
   */
  protected implicit class TraversableNode(node: Node) {

    /**
     * creates a traversal Iterable starting with Node node
     * stopEval and RetEval are called with (Nodes of) type T.
     * The returned Iterable can be converted to List. F. e.:
     *
     * <pre><code>
     * myNode.doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY" -<- "FOO") {
     * END_OF_GRAPH
     * } {
     * case (x: Matrix, tp) if (tp.depth == 2) => x.name.length > 2
     * case (x: NonMatrix, _) => false
     * }.toList.sortWith(_.name < _.name)
     * </code></pre>
     *
     * @tparam T result type (nodes are converted to T)
     * @param rb RelationBuffer to define Directions and RelationTypes
     * @param stopEval Partial Function for stop evaluator function
     * @param retEval Partial Function for return evaluator function
     * @return Iterable[T] of converted nodes of type T
     */
    def doTraverse[T: Manifest](rb: RelationBuffer)
                               (stopEval: PartialFunction[(T, TraversalPosition), Boolean])
                               (retEval: PartialFunction[(T, TraversalPosition), Boolean]): Iterable[T] = {
      val traverser = node.traverse(rb.getOrder,
        (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
          case Some(x) if (stopEval.isDefinedAt(x, tp)) => stopEval(x, tp)
          case _ => false
        },
        (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
          case Some(x) if (retEval.isDefinedAt(x, tp)) => retEval(x, tp)
          case _ => false
        }, rb.get: _*)

      import collection.JavaConversions.asScalaIterator
      new TypedPropertyContainerIterator[T](traverser.iterator)
    }
  }

  /**
   * Enhances a List of Nodes with a doTraverse method
   * @param list list of nodes
   */
  implicit class TraversableNodeList(list: List[Node]) {

    /**
     * creates a traversal Iterable starting traversals for every Node in the list
     * stopEval and RetEval are called with (Nodes of) type T.
     * The returned Iterable can be converted to List. F. e.:
     *
     * <pre><code>
     * myListOfNodes.doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY" -<- "FOO") {
     * END_OF_GRAPH
     * } {
     * case (x: Matrix, tp) if (tp.depth == 2) => x.name.length > 2
     * case (x: NonMatrix, _) => false
     * }.toList.sortWith(_.name < _.name)
     * </code></pre>
     *
     * All Nodes are preconverted to a List[T] and distinct
     *
     * @tparam T result type (nodes are converted to T)
     * @param rb RelationBuffer to define Directions and RelationTypes
     * @param stopEval Partial Function for stop evaluator function
     * @param retEval Partial Function for return evaluator function
     * @return Iterable[T] of converted nodes of type T
     */
    def doTraverse[T: Manifest](rb: RelationBuffer)
                               (stopEval: PartialFunction[(T, TraversalPosition), Boolean])
                               (retEval: PartialFunction[(T, TraversalPosition), Boolean]): Iterable[T] = {
      val nodes = list.par.map {
        _.doTraverse[T](rb)(stopEval)(retEval).toList
      }.flatMap(l => l).distinct

      new Iterable[T] {
        def iterator = nodes.iterator
      }
    }
  }
}