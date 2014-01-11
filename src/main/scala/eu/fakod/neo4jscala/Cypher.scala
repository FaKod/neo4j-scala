package eu.fakod.neo4jscala

import scala.language.implicitConversions
import org.neo4j.cypher.{PlanDescription, ExecutionResult, ExecutionEngine}
import java.io.PrintWriter
import org.neo4j.graphdb.PropertyContainer

/**
 * add additional as[T] interface where T has to be
 * a case class
 */
trait TypedExecutionResult extends ExecutionResult {
  /**
   * maps a given column that has to be a property container
   * to a case class
   * @param column name of the column
   * @tparam T type of case class
   * @return Iterator[T]
   */
  def asCC[T: Manifest](column: String): Iterator[T]
}

/**
 * Wraps the Cypher class ExecutionResult and provides an additional
 * Interface to access unmarshaled classes
 * @param er ExecutionResult original execution result
 */
class TypedExecutionResultImpl(val er: ExecutionResult) extends TypedExecutionResult {
  def hasNext = er.hasNext

  def next = er.next

  def columns = er.columns

  def javaColumns = er.javaColumns

  def javaColumnAs[T](column: String) = er.javaColumnAs[T](column)

  def columnAs[T](column: String) = er.columnAs[T](column)

  def javaIterator = er.javaIterator

  def dumpToString(writer: PrintWriter) {
    er.dumpToString(writer)
  }

  def dumpToString = er.dumpToString

  def queryStatistics = er.queryStatistics

  def executionPlanDescription(): PlanDescription = er.executionPlanDescription()

  def close(): Unit = er.close()

  /**
   * maps a given column that has to be a property container
   * to a case class
   * @param column name of the column
   * @tparam T type of case class
   * @return Iterator[T]
   */
  def asCC[T: Manifest](column: String): Iterator[T] = {
    new TypedPropertyContainerIterator(er.columnAs[PropertyContainer](column)).iterator
  }

}

/**
 * main Cypher support trait
 */
trait Cypher {

  self: Neo4jWrapper =>

  lazy val engine = new ExecutionEngine(ds.gds)

  implicit class CypherQueryExecutor(query: String) {
    def execute: TypedExecutionResult = new TypedExecutionResultImpl(engine.execute(query))
  }

}
