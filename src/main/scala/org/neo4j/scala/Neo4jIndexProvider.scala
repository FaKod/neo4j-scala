package org.neo4j.scala

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.index.{Index, RelationshipIndex}
import scala.collection.mutable.{Map => mutableMap}
import collection.JavaConversions._

/**
 * Provides Index access as trait
 *
 * @author Christopher Schmidt
 * Date: 11.04.11
 * Time: 20:27
 */

trait Neo4jIndexProvider {

  /**
   * type convenience definition
   */
  type IndexCustomConfig = Option[Map[String, String]]

  /**
   * required DatabaseService provided by XXXServiceProvider
   */
  val ds: DatabaseService

  /**
   * has to be overwritten to define Node Index and configuration
   */
  def NodeIndexConfig: List[(String, IndexCustomConfig)] = Nil

  /**
   * has to be overwritten to define Relation Index and configuration
   */
  def RelationIndexConfig: List[(String, IndexCustomConfig)] = Nil

  /**
   * private cache
   */
  private val nodeIndexStore = mutableMap[String, Index[Node]]()

  /**
   * private cache
   */
  private val relationIndexStore = mutableMap[String, RelationshipIndex]()

  /**
   * constructor creates indexes
   */
  for (forNode <- NodeIndexConfig) {
    nodeIndexStore += forNode._1 ->
      (forNode._2 match {
        case Some(config) => getIndexManager.forNodes(forNode._1, config)
        case _ => getIndexManager.forNodes(forNode._1)
      })
  }

  for (forRelation <- RelationIndexConfig) {
    relationIndexStore += forRelation._1 ->
      (forRelation._2 match {
        case Some(config) => getIndexManager.forRelationships(forRelation._1, config)
        case _ => getIndexManager.forRelationships(forRelation._1)
      })
  }

  /**
   * returns the index manager
   * @return IndexManager the index manager
   */
  def getIndexManager = ds.gds.index

  /**
   * @return Option[Index[Node]] the created index if available
   */
  def getNodeIndex(name: String) = nodeIndexStore.get(name)

  /**
   * @return Option[RelationshipIndex] the created index if available
   */
  def getRelationIndex(name: String) = relationIndexStore.get(name)

  /**
   * conversion to ease the use of optional configuration
   */
  implicit def mapToOptionMap(t:(String, Map[String, String])) = (t._1, Option(t._2))

}