package org.neo4j.scala

import org.neo4j.kernel.impl.batchinsert.BatchInserter
import java.util.{Map => juMap}
import org.neo4j.graphdb.index._
import org.neo4j.graphdb._
import org.neo4j.index.impl.lucene.{AbstractIndexHits, LuceneBatchInserterIndexProvider}
import collection.JavaConversions._
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import collection.mutable.{SynchronizedMap, ConcurrentMap, HashMap}

/**
 * provides Index access trait
 * class must mixin a trait that provides an instance of class BatchInserter
 * i.g. BatchGraphDatabaseServiceProvider
 */
trait Neo4jBatchIndexProvider extends Neo4jIndexProvider {

  /**
   * instance of BatchInserter
   */
  def batchInserter: BatchInserter


  private val batchIndexManager = new BatchIndexManager(batchInserter)

  /**
   * delegates to shutdown method
   */
  def shutdownIndex = batchIndexManager.shutdown

  /**
   * store for IndexManager
   */
  override def getIndexManager: IndexManager = batchIndexManager

  /**
   * converts implicitly to the underlying batch instance
   */
  implicit def nodeIndexToBatchIndex(ni: Index[Node]) = ni.asInstanceOf[BatchIndex]

  /**
   * converts implicitly to the underlying batch instance
   */
  implicit def relationIndexToBatchRelationshipIndex(ri: RelationshipIndex) = ri.asInstanceOf[BatchRelationshipIndex]
}


/**
 * delegated methods of IndexManager to BatchInserter
 */
class BatchIndexManager(bi: BatchInserter) extends IndexManager {

  /**
   * instance of LuceneBatchInserterIndexProvider
   */
  private val batchInserterIndexProvider: BatchInserterIndexProvider = new LuceneBatchInserterIndexProvider(bi)


  def forNodes(indexName: String, customConfiguration: juMap[String, String]) =
    new BatchIndex(batchInserterIndexProvider.nodeIndex(indexName, customConfiguration), bi)

  def forRelationships(indexName: String, customConfiguration: juMap[String, String]) =
    new BatchRelationshipIndex(batchInserterIndexProvider.relationshipIndex(indexName, customConfiguration), bi)

  /**
   * Shuts down this index provider and ensures that all indexes are fully
   * written to disk.
   */
  def shutdown = batchInserterIndexProvider.shutdown

  def existsForNodes(indexName: String) = throw new NotImplementedException

  def forNodes(indexName: String) = throw new NotImplementedException

  def nodeIndexNames() = throw new NotImplementedException

  def existsForRelationships(indexName: String) = throw new NotImplementedException

  def forRelationships(indexName: String) = throw new NotImplementedException

  def relationshipIndexNames() = throw new NotImplementedException

  def getConfiguration(index: Index[_ <: PropertyContainer]) = throw new NotImplementedException

  def setConfiguration(index: Index[_ <: PropertyContainer], key: String, value: String) = throw new NotImplementedException

  def removeConfiguration(index: Index[_ <: PropertyContainer], key: String) = throw new NotImplementedException

  def getNodeAutoIndexer = throw new NotImplementedException

  def getRelationshipAutoIndexer = throw new NotImplementedException
}

private[scala] trait IndexCacheHelper {

  private val cache = new HashMap[Long, HashMap[String, AnyRef]] with SynchronizedMap[Long, HashMap[String, AnyRef]]

  /**
   * caches multible values
   */
  protected def addToCache(id: Long, key: String, value: AnyRef) =
    cache.getOrElseUpdate(id, HashMap[String, AnyRef]()) += ((key, value))

  protected def cacheClear = cache.clear
}

/**
 * delegates Index[Node] methods to BatchInserterIndex methods
 */
class BatchIndex(bii: BatchInserterIndex, bi: BatchInserter) extends Index[Node] with IndexCacheHelper {

  private val gds = bi.getGraphDbService

  /**
   * implicitly converts IndexHits[Long] to IndexHits[BatchNode]
   */
  private implicit def toNodeIndexHits(hits: IndexHits[java.lang.Long]): IndexHits[Node] = {
    val listOfNodes = for (l <- hits.iterator) yield gds.getNodeById(l)
    new ConstantScoreIterator[Node](listOfNodes.toList)
  }

  def updateOrAdd(entityId: Long, properties: Map[String, AnyRef]) = bii.updateOrAdd(entityId, properties)

  def flush = {
    cacheClear
    bii.flush
  }

  def setCacheCapacity(key: String, size: Int) = bii.setCacheCapacity(key, size)

  /**
   * uses the implementation that removes existing documents
   * and replaces them with the cached ones
   */
  def add(node: Node, key: String, value: AnyRef) =
    bii.updateOrAdd(node.getId, addToCache(node.getId, key, value))

  def get(key: String, value: AnyRef) = bii.get(key, value)

  def query(key: String, queryOrQueryObject: AnyRef) = bii.query(key, queryOrQueryObject)

  def query(queryOrQueryObject: AnyRef) = bii.query(queryOrQueryObject)

  def isWriteable = false

  def remove(entity: Node, key: String, value: AnyRef) {
    throw new NotImplementedException
  }

  def remove(entity: Node, key: String) {
    throw new NotImplementedException
  }

  def remove(entity: Node) {
    throw new NotImplementedException
  }

  def delete() {
    throw new NotImplementedException
  }

  def getName = throw new NotImplementedException

  def getEntityType = throw new NotImplementedException
}

/**
 * delegates RelationshipIndex methods to BatchInserterIndex methods
 */
class BatchRelationshipIndex(bii: BatchInserterIndex, bi: BatchInserter) extends RelationshipIndex with IndexCacheHelper {

  private val gds = bi.getGraphDbService

  /**
   * implicitly converts IndexHits[Long] to IndexHits[BatchRelationship]
   */
  private implicit def toRelationshipIndexHits(hits: IndexHits[java.lang.Long]): IndexHits[Relationship] = {
    val listOfNodes = for (l <- hits.iterator) yield gds.getRelationshipById(l)
    new ConstantScoreIterator[Relationship](listOfNodes.toList)
  }

  def updateOrAdd(entityId: Long, properties: Map[String, AnyRef]) = bii.updateOrAdd(entityId, properties)

  def flush = {
    cacheClear
    bii.flush
  }

  def setCacheCapacity(key: String, size: Int) = bii.setCacheCapacity(key, size)

  def add(entity: Relationship, key: String, value: AnyRef) =
    bii.updateOrAdd(entity.getId, addToCache(entity.getId, key, value))

  def get(key: String, value: AnyRef) = bii.get(key, value)

  def query(key: String, queryOrQueryObject: AnyRef) = bii.query(key, queryOrQueryObject)

  def query(queryOrQueryObject: AnyRef) = bii.query(queryOrQueryObject)

  def isWriteable = false

  def remove(entity: Relationship, key: String, value: AnyRef) {
    throw new NotImplementedException
  }

  def remove(entity: Relationship, key: String) {
    throw new NotImplementedException
  }

  def remove(entity: Relationship) {
    throw new NotImplementedException
  }

  def delete() {
    throw new NotImplementedException
  }

  def getName = throw new NotImplementedException

  def getEntityType = throw new NotImplementedException

  def get(key: String, valueOrNull: AnyRef, startNodeOrNull: Node, endNodeOrNull: Node) = throw new NotImplementedException

  def query(key: String, queryOrQueryObjectOrNull: AnyRef, startNodeOrNull: Node, endNodeOrNull: Node) = throw new NotImplementedException

  def query(queryOrQueryObjectOrNull: AnyRef, startNodeOrNull: Node, endNodeOrNull: Node) = throw new NotImplementedException
}

/**
 * replica of the original ConstantScoreIterator which has package visibility
 * class org.neo4j.index.impl.lucene.ConstantScoreIterator<T> extends AbstractIndexHits<T>
 */
class ConstantScoreIterator[T](items: List[T], score: Float = Float.NaN) extends AbstractIndexHits[T] {

  private final val _size: Int = items.size
  private final val iter = items.iterator

  def currentScore: Float = score

  def size: Int = _size

  protected def fetchNextOrNull: T =
    if (iter.hasNext)
      iter.next
    else
      null.asInstanceOf[T]
}