package eu.fakod.neo4jscala

import scala.language.implicitConversions
import org.neo4j.graphdb.{TraversalPosition, Node}
import eu.fakod.rest.graphdb.traversal.RestOldTraverserWrapper


trait RestTypedTraverser extends TypedTraverserBase {
  self: Neo4jWrapper =>

  /**
   * Language enumeration for prune evaluator
   */
  object ScriptLanguage extends Enumeration {
    type ScriptLanguage = Value
    val JAVASCRIPT, BUILTIN = Value
  }

  import ScriptLanguage._

  /**
   * Prune Evaluator in a certain language to be used server side.
   * F.e.
   * <code>
   * position.endNode().getProperty('date')>1234567;
   * </code>
   * position is of type @see org.neo4j.graphdb.Path
   *
   * @param body String the code
   * @param language String defaults to Java Script
   */
  case class PruneEvaluator(body: String, language: ScriptLanguage = JAVASCRIPT)


  // Convenience methods for PruneEvaluator handling
  implicit def toEitherPruneEvaluator(pe: PruneEvaluator): Either[PruneEvaluator, Int] = Left(pe)

  // Convenience for String to Prune Evaluator
  implicit def toEitherPruneEvaluator(js: String): Either[PruneEvaluator, Int] = Left(PruneEvaluator(js))

  // Convenience for Int to Either MaxDepth
  implicit def toEitherInt(maxDepth: Int): Either[PruneEvaluator, Int] = Right(maxDepth)


  /**
   * Return Filter in a certain language to be used on server side.
   * If ScriptLanguage is BUILTIN the body attribute is used as name of the builtin filter
   * @param body String the code
   * @param language String defaults to Java Script
   */
  case class ReturnFilter(body: String, language: ScriptLanguage = JAVASCRIPT)

  // Convenience method for String to Return Filter
  implicit def toReturnFilter(rf: String): ReturnFilter = ReturnFilter(rf)

  /**
   * Builtin filter to return all nodes
   */
  val ReturnAll = ReturnFilter("all", BUILTIN)

  /**
   * Builtin filter to return all but start node
   */
  val ReturnAllButStartNode = ReturnFilter("all_but_start_node", BUILTIN)


  /**
   * helper class for creating Java Script code
   * @param js String Java Script code to prepend
   */
  class JavaScriptForReturnFilter(val js: String) {
    def isOfType[T: Manifest] = js + isOfCaseClass_JS[T]
  }

  /**
   * <code>
   * ReturnFilter("position.endNode()" + isOfCaseClass_JS[Matrix])
   * </code>
   *
   * @tparam T type of case class to check
   * @return String Java Script code snippet to be used for ReturnFilter or PruneEvaluator
   */
  def isOfCaseClass_JS[T: Manifest] =
    ".getProperty('" + Neo4jWrapper.ClassPropertyName + "')==\"" + manifest[T].runtimeClass.getName + "\";"

  /**
   * creates Java Script code for server side startNode type test
   * @return String JS code
   */
  def startNode = new JavaScriptForReturnFilter("position.startNode()")

  /**
   * creates Java Script code for server side endNode type test
   * @return String JS code
   */
  def endNode = new JavaScriptForReturnFilter("position.endNode()")


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
     * PruneEvaluator("position.length() > 100;")
     * } {
     * case (x: Matrix, tp) if (tp.depth == 2) => x.name.length > 2
     * case (x: NonMatrix, _) => false
     * }.toList.sortWith(_.name < _.name)
     * </code></pre>
     *
     * @tparam T result type (nodes are converted to T)
     * @param rb RelationBuffer to define Directions and RelationTypes
     * @param stopEval Either Java Script (@see org.neo4j.graphdb.Path) code or Max Depth as integer
     * @param retEval Partial Function for return evaluator function
     * @return Iterable[T] of converted nodes of type T
     */
    def doTraverse[T: Manifest](rb: RelationBuffer)
                               (stopEval: Either[PruneEvaluator, Int])
                               (retEval: PartialFunction[(T, TraversalPosition), Boolean]): Iterable[T] = {

      val traverser = stopEval match {
        case Left(pruneEval) =>
          RestOldTraverserWrapper.traverse(node, rb.getOrder, pruneEval.body, pruneEval.language.toString,
            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
              case Some(x) if retEval.isDefinedAt((x, tp)) => retEval((x, tp))
              case _ => false
            }, rb.get: _*)

        case Right(maxDepth) =>
          RestOldTraverserWrapper.traverse(node, rb.getOrder, maxDepth,
            (tp: TraversalPosition) => tp.currentNode.toCC[T] match {
              case Some(x) if retEval.isDefinedAt((x, tp)) => retEval((x, tp))
              case _ => false
            }, rb.get: _*)
      }

      import collection.JavaConversions.asScalaIterator
      new TypedPropertyContainerIterator[T](traverser.iterator)
    }


    /**
     * Simplified method to use a Prune Evaluator or Max Depth and
     * a Java Script or builtin Return Filter
     *
     * @param rb RelationBuffer to define Directions and RelationTypes
     * @param stopEval Either Java Script (@see org.neo4j.graphdb.Path) code or Max Depth as integer
     * @param retEval ReturnFilter Java Script Code
     * @tparam T result type (nodes are converted to T)
     * @return Iterable[T] of converted nodes of type T
     */
    def doTraverse[T: Manifest](rb: RelationBuffer,
                                stopEval: Either[PruneEvaluator, Int],
                                retEval: ReturnFilter): Iterable[T] = {

      val traverser = stopEval match {
        case Left(pruneEval) =>
          RestOldTraverserWrapper.traverse(node, rb.getOrder, pruneEval.body, pruneEval.language.toString,
            retEval.body, retEval.language.toString, rb.get: _*)

        case Right(maxDepth) =>
          RestOldTraverserWrapper.traverse(node, rb.getOrder, maxDepth,
            retEval.body, retEval.language.toString, rb.get: _*)
      }

      import collection.JavaConversions.asScalaIterator
      new TypedPropertyContainerIterator[T](traverser.iterator)
    }
  }

  /**
   * Enhances a List of Nodes with a doTraverse method
   * @param list list of nodes
   */
  implicit def nodeListToTraverse(list: List[Node]) = new {

    /**
     * creates a traversal Iterable starting traversals for every Node in the list
     * stopEval and RetEval are called with (Nodes of) type T.
     * The returned Iterable can be converted to List. F. e.:
     *
     * <pre><code>
     * myListOfNodes.doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY" -<- "FOO") {
     * "position.length() > 100;"
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
     * @param stopEval Either Java Script (@see org.neo4j.graphdb.Path) code or Max Depth as integer
     * @param retEval Partial Function for return evaluator function
     * @return Iterable[T] of converted nodes of type T
     */
    def doTraverse[T: Manifest](rb: RelationBuffer)
                               (stopEval: Either[PruneEvaluator, Int])
                               (retEval: PartialFunction[(T, TraversalPosition), Boolean]): Iterable[T] = {
      val nodes = list.par.map {
        _.doTraverse[T](rb)(stopEval)(retEval).toList
      }.flatMap(l => l).distinct

      new Iterable[T] {
        def iterator = nodes.iterator
      }
    }

    /**
     * Simplified method to use a Prune Evaluator or Max Depth and
     * a Java Script or builtin Return Filter
     *
     * @param rb RelationBuffer to define Directions and RelationTypes
     * @param stopEval Either Java Script (@see org.neo4j.graphdb.Path) code or Max Depth as integer
     * @param retEval ReturnFilter Java Script Code
     * @tparam T result type (nodes are converted to T)
     * @return Iterable[T] of converted nodes of type T
     */
    def doTraverse[T: Manifest](rb: RelationBuffer,
                                stopEval: Either[PruneEvaluator, Int],
                                retEval: ReturnFilter): Iterable[T] = {
      val nodes = list.par.map {
        _.doTraverse[T](rb, stopEval, retEval).toList
      }.flatMap(l => l).distinct

      new Iterable[T] {
        def iterator = nodes.iterator
      }
    }
  }

}
