#Neo4j Scala wrapper library

The Neo4j Scala wrapper library allows you the [Neo4j open source graph database](http://neo4j.org/) through a
domain-specific simplified language. It is written in Scala and is intended
to be used in other Scala projects.

This wrapper is mostly based on the work done by [Martin Kleppmann](http://twitter.com/martinkl) in his [Scala implementation of RESTful JSON HTTP resources on top of the Neo4j graph database and Jersey](http://github.com/ept/neo4j-resources) project.



See this [GIST](https://gist.github.com/1331556) for a usual Neo4j Matrix Example

You may find [Neo4j-Spatial-Scala](http://github.com/FaKod/neo4j-spatial-scala) interesting as well.


##Building

    $ git clone git://github.com/FaKod/neo4j-scala.git
    $ cd neo4j-scala
    $ mvn clean install

Or try to maven fetch it with a Github Maven Repo:

	    <repositories>
	      <repository>
	        <id>fakod-snapshots</id>
	        <url>https://raw.github.com/FaKod/fakod-mvn-repo/master/snapshots</url>
	      </repository>
	      <repository>
               <id>fakod-releases</id>
               <url>https://raw.github.com/FaKod/fakod-mvn-repo/master/releases</url>
              </repository>
	    </repositories>

	    <dependencies>
	      <dependency>
	        <groupId>org.neo4j</groupId>
	        <artifactId>neo4j-scala</artifactId>
	        <version>0.1.0</version>
	      </dependency>
	    </dependencies>

##Troubleshooting

Please consider using [Github issues tracker](http://github.com/fakod/neo4j-scala/issues) to submit bug reports or feature requests.


#Using this library

##Graph Database Service Provider

Neo4j Scala Wrapper needs a Graph Database Service Provider, it has to implement GraphDatabaseServiceProvider trait.
One possibility is to use the EmbeddedGraphDatabaseServiceProvider for embedded Neo4j instances where you simply have to define a Neo4j storage directory.
The class MyNeo4jClass using the wrapper is f.e.:

    class MyNeo4jClass extends SomethingClass with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider {
      def neo4jStoreDir = "/tmp/temp-neo-test"
      . . .
    }

Available are:

* EmbeddedGraphDatabaseServiceProvider
* SingletonEmbeddedGraphDatabaseServiceProvider (singleton version)
* BatchGraphDatabaseServiceProvider (use it with Neo4jBatchIndexProvider)
* RestGraphDatabaseServiceProvider uses the REST binding (0.2.0-SNAPSHOT only)

##Transaction Wrapping

Transactions are wrapped by withTx. After leaving the "scope" success is called (or rollback if an exception is raised):

    withTx {
     implicit neo =>
       val start = createNode
       val end = createNode
       start --> "foo" --> end
    }

##Using an Index

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

##Relations


Using this wrapper, this is how creating two relationships can look in Scala. 
The String are automatically converted into Dynamic Relationsships:

    start --> "KNOWS" --> intermediary --> "KNOWS" --> end
    left --> "foo" --> middle <-- "bar" <-- right

To return the Property Container for the Relation Object use the '<' method:

    val relation = start --> "KNOWS" --> end <

##Properties

And this is how getting and setting properties on a node or relationship looks like :

    // setting the property foo
    start("foo") = "bar"
    // cast Object to String and match . . .
    start[String]("foo") match {
    	case Some(x) => println(x)
	    case None => println("aww")
    }

##Using Case Classes

Neo4j provides storing keys (String) and values (Object) into Nodes. To store Case Classes the properties are stored as key/values to the Property Container, thai can be a Node or a Relation. However, Working types are limited to basic types like String, integer etc.

    case class Test(s: String, i: Int, ji: java.lang.Integer, d: Double, l: Long, b: Boolean)

    . . .
    withTx {
      implicit neo =>
        // create new Node with Case Class Test
        val node1 = createNode(Test("Something", 1, 2, 3.3, 10, true))

        // can Test be created from node
        val b:Boolean = node.toCCPossible[Test]

        // or using Option[T] (returning Some[T] if possible)
        val nodeOption: Option[Test] = node.toCC[Test]
 
        // yield all Nodes that are of type Case Class Test
		val tests = for(n <- getTraverser; t <- n.toCC[Test]) yield t
		
		// create new relation with Case Class Test
		node1 --> "foo" --> node2 < Test("other", 0, 1, 1.3, 1, false)
    }

##Traversing

Besides, the neo4j scala binding makes it possible to write stop and returnable evaluators in a functional style :

    //StopEvaluator.END_OF_GRAPH, written in a Scala idiomatic way :
    start.traverse(Traverser.Order.BREADTH_FIRST, (tp : TraversalPosition) => false, 
		ReturnableEvaluator.ALL_BUT_START_NODE, "foo", Direction.OUTGOING)
    
    //ReturnableEvaluator.ALL_BUT_START_NODE, written in a Scala idiomatic way :
    start.traverse(Traverser.Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, (tp : TraversalPosition) => tp.notStartNode(), 
		"foo", Direction.OUTGOING)

##Typed Traversing (only Version 0.2.0-SNAPSHOT)

The traverser mentioned above processes and returns Nodes resp. Property Container. To allow a more type safe traverser the TypedTraverser was introduced. The basic semantic is that you have to define the type (a case class) that should be returned (while inheritance is respected).
But first define Relation Types and Directions:

###Relation Types and Direction

To define relation types and directions use the follow method. Some examples

	follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY" // BOTH for "KNOWS", OUTGOING for "CODED_BY"
	follow -<- "BAR" -- "FOO"                       // INCOMING for "BAR", BOTH for "FOO", defaults to DEPTH_FIRST
	follow(DEPTH_FIRST) ->- "FOOBAR"                // OUTGOING for "FOOBAR"

So the traverser, that returns an Iterable[MatrixBase], can be written like this:

	myNode.doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY") {
	    . . .block1. . .
	} {
	    . . .block2. . .
	}

###Return and Stop Evaluator

**block1** is the Stop Evaluator and **block2** the Return Evaluator, both of type PartialFunction[(T, TraversalPosition), Boolean]. Where T is MatrixBase in case of the example, TraversalPosition from the traverser and Boolean as return type. 

PartialFunctions can be handled with a case statement, like this:

	node.doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY") {
    	END_OF_GRAPH // predefined partial function
  	} {
		// if node is of type Matrix and TraversalPosition.depth == 2 then check lenth
    	case (x: Matrix, tp) if (tp.depth == 2) => x.name.length > 2

		// if node is of type NonMatrix then false
    	case (y: NonMatrix, _) => false
  	}

Assuming that Matrix and NonMatrix are inherited from MatrixBase the traverser will handle that correctly. X and y only matches if the given type can be marshaled to Matrix or NonMatrix and assigned from MatrixBase. Additionally, you can use the TraversalPosition in both case statements or ignore it with '_'. The example defines that depth must be 2 and the Matrix.name parameter must be of length > 2. NonMatrix classes are not returned. The Stop Evaluator always returns false.

###Iterable[T]

The example above returns an Iterable[MatrixBase]. This allows to use the powerful Scala collections, f.e.:

	...}.toList.sortWith(_.name < _.name) // sorts Nodes by name
	
	...}.toList.flatMap(_.name) // List(N, e, o, M, o, r, p, h, e, u, s)
	
	...}.toList.par. . . // do something with parallel collections
	
Where '_' is automatically replaced by MatrixBase instances.
	
Finally we can write, f.e.:

	val list:List[MatrixBase] = node.doTraverse[MatrixBase](follow -<- "KNOWS") {
	    case _ => false
	} {
	    case (x: Matrix, _) => true
	    case (x: NonMatrix, _) => false
	}.toList.sortWith(_.name < _.name)

###Using a List of Nodes

Instead of one Node you can use a List of Nodes (List[Node]). The given traverser is started multithreaded for every node in the List. The resulting threads are joined, the result-lists are appended and Node duplicates removed. F.e:

	val erg1 = startWithNodes.doTraverse[MatrixBase](follow -<- "KNOWS") {
	    case _ => false
	  } {
	    case (x: Matrix, _) => true
	    case (x: NonMatrix, _) => false
	  }.toList.sortWith(_.name < _.name)

Where startWithNodes is of type List[Node].


##Batch Processing

Neo4j has a batch insertion mode intended for initial imports, which must run in a single thread and bypasses transactions and other checks in favor of performance. See [Batch insertion](http://docs.neo4j.org/chunked/milestone/indexing-batchinsert.html).

The Java interfaces are slightly different. I wrote some wrapper classes to support nearly transparent usage of batch node and batch relation insertion. Means same code for batch insertion and for normal non batch mode. Instead of using

    class Builder extends Neo4jWrapper with SingletonEmbeddedGraphDatabaseServiceProvider with Neo4jIndexProvider {...}

simply exchange the provider traits with

    class Builder extends Neo4jWrapper with Neo4jBatchIndexProvider with BatchGraphDatabaseServiceProvider {...}

getting the indexes is still the same code

    val nodeIndex = getNodeIndex("NodeIndex").get
	val relationIndex = getRelationIndex("RelationIndex").get
	
setting cache size:

	nodeIndex.setCacheCapacity("NodeIndex", 1000000)
	relationIndex.setCacheCapacity("RelationIndex", 1000000)
	
Nevertheless, indexes are not available till flushing. To flush call:

    nodeIndex.flush
	relationIndex.flush

After insertion, the batch index manager and batch insertion manager have to be shut down

    class Builder extends Neo4jWrapper . . .{
	   . . .
	   shutdownIndex
	   shutdown(ds)
	   . . .
	}