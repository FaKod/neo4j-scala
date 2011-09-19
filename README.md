Neo4j Scala wrapper library
=======================

The Neo4j Scala wrapper library allows you the [Neo4j open source graph database](http://neo4j.org/) through a
domain-specific simplified language. It is written in Scala and is intended
to be used in other Scala projects.

This wrapper is mostly based on the work done by [Martin Kleppmann](http://twitter.com/martinkl) in his [Scala implementation of RESTful JSON HTTP resources on top of the Neo4j graph database and Jersey](http://github.com/ept/neo4j-resources) project.


You may find [Neo4j-Spatial-Scala](http://github.com/FaKod/neo4j-spatial-scala) interesting as well.


Building
--------

    $ git clone git://github.com/fakod/neo4j-scala.git
    $ cd neo4j-scala
    $ mvn clean install

Troubleshooting
---------------

Please consider using [Github issues tracker](http://github.com/fakod/neo4j-scala/issues) to submit bug reports or feature requests.


Using this library
==================

Graph Database Service Provider
------------------------------
Neo4j Scala Wrapper needs a Graph Database Service Provider, it has to implement GraphDatabaseServiceProvider trait.
One possibility is to use the EmbeddedGraphDatabaseServiceProvider for embedded Neo4j instances where you simply have to define a Neo4j storage directory.
A class using the wrapper is f.e.:

    class MyNeo4jClass extends SomethingClass with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider {
      def neo4jStoreDir = "/tmp/temp-neo-test"
      . . .
    }

Transaction Wrapping
--------------------
Transactions are wrapped by withTx. After leaving the "scope" success is called (or rollback if an exception is raised):

    withTx {
     implicit neo =>
       val start = createNode
       val end = createNode
       start --> "foo" --> end
    }

Using an Index
---------------------
Neo4j provides indexes for nodes and relationships. The indexes can be configured by mixing in the Neo4jIndexProvider trait. See [Indexing](http://docs.neo4j.org/chunked/stable/indexing.html)

    class MyNeo4jClass extends . . . with Neo4jIndexProvider {
      // configuration for the index being created.
      override def NodeIndexConfig = ("MyTest1stIndex", Map("provider" -> "lucene", "type" -> "fulltext")) ::
                                     ("MyTest2ndIndex", Map("provider" -> "lucene", "type" -> "fulltext")) :: Nil
    }

Use one of the configured indexes with

    val nodeIndex = getNodeIndex("MyTest1stIndex").get

Add and remove entries by:

    nodeIndex += (Node_A, "title", "The Matrix")
    nodeIndex -= (Node_A)

Relations
---------

Using this wrapper, this is how creating two relationships can look in Scala:

    start --> "KNOWS" --> intermediary --> "KNOWS" --> end
    left --> "foo" --> middle <-- "bar" <-- right

Returning the Relation Object:

    val relation = start --> "KNOWS" --> end <;

Properties
----------

And this is how getting and setting properties on a node or relationship looks like :

    // setting the property foo
    start("foo") = "bar"
    // cast Object to String and match . . .
    start[String]("foo") match {
    	case Some(x) => println(x)
	    case None => println("aww")
    }

Using Case Classes
------------------
Neo4j provides storing keys (String) and values (Object) into Nodes. To store Case Classes the property names of the case class are used as keys and the values are stored Strings as well. Working types are limited to basic types like String, integer etc.

    case class Test(s: String, i: Int, ji: java.lang.Integer, d: Double, l: Long, b: Boolean)
    . . .
    withTx {
      implicit neo =>
        // create Node with Case Class Test
        val node1 = createNode(Test("Something", 1, 2, 3.3, 10, true))

        // "recreate" Case Class Test from Node
        val node2 = Neo4jWrapper.deSerialize[Test](node)

        // or using Option[T] (returning Some[T] if possible)
        val nodeOption: Option[Test] = node.toCC[Test]
 
        // yield all Nodes that are of Case Class Test
		val tests = for(n <- getTraverser; t <- n.toCC[Test]) yield t
    }

Traversing
----------

Besides, the neo4j scala binding makes it possible to write stop and returnable evaluators in a functional style :

    //StopEvaluator.END_OF_GRAPH, written in a Scala idiomatic way :
    start.traverse(Traverser.Order.BREADTH_FIRST, (tp : TraversalPosition) => false, ReturnableEvaluator.ALL_BUT_START_NODE, "foo", Direction.OUTGOING)
    
    //ReturnableEvaluator.ALL_BUT_START_NODE, written in a Scala idiomatic way :
    start.traverse(Traverser.Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, (tp : TraversalPosition) => tp.notStartNode(), "foo", Direction.OUTGOING)


