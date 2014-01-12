#Neo4j Scala wrapper library

```
Version 0.3.0...:
- I had to refactor the packages to my own domain, to allow deployment to Sonatype Maven repository.
- Snapshots aren't synchronized with Central. So use Sonatype repo for now (see below)
- "old" version is still in the 0.2.0-M git branch.
- further developments in master branch
```

The Neo4j Scala wrapper library allows you use [Neo4j open source graph database](http://neo4j.org/) through a domain-specific language.

This wrapper is mostly based on the work done by [Martin Kleppmann](http://twitter.com/martinkl) in his [Scala implementation of RESTful JSON HTTP resources on top of the Neo4j graph database and Jersey](http://github.com/ept/neo4j-resources) project.



See this [GIST](https://gist.github.com/1331556) for a usual Neo4j Matrix Example

You may find [Neo4j-Spatial-Scala](http://github.com/FaKod/neo4j-spatial-scala) interesting as well.

All discussions (if there are any) see Google Group [neo4j-scala](https://groups.google.com/forum/#!forum/neo4j-scala)


##Building

    $ git clone git://github.com/FaKod/neo4j-scala.git
    $ cd neo4j-scala
    $ mvn clean install

Or fetch it with Maven (the Sonatype Maven Repo is only needed if you want to use a SNAPSHOT version):

```xml
<repositories>
  <repository>
    <id>sonatype-snapshots</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
  </repository>
  ...
</repositories>

<dependencies>
  <dependency>
    <groupId>eu.fakod</groupId>
    <artifactId>neo4j-scala_2.10</artifactId>
    <version>0.3.0</version>
  </dependency>
</dependencies>
```

##Troubleshooting

Please consider using [Github issues tracker](http://github.com/fakod/neo4j-scala/issues) to submit bug reports or feature requests.

#Versions

##0.3.1-SNAPSHOT
* Thanks to Alexander Korneev: fixes such warnings by replacing implicit methods that return structural types, with implicit classes. It also fixes some other warnings that arise when compiling the library itself.

##0.3.0
* Based on 0.2.0-M3-SNAPSHOT
* **Refactoring from org.neo4j.scala to eu.fakod.neo4jscala**.
* "Old"" version is still in the 0.2.0-M git branch
* using artifact id **neo4j-scala_2.10** now
* deployed to central

##0.2.0-M3-SNAPSHOT
* Scala 2.10.3
* Neo4j 1.9.4

##0.2.0-M2-SNAPSHOT

* Switched to Neo4j Version 1.8
* Added simple Cypher Support
* Added programmatic access to Configuration Parameter
* Using incremental Scala compiler now

##0.2.0-M1

* Switched to Neo4j Version 1.7
* Introducing Typed Traverser for type safe traversals
* Added REST Graph DB Provider to support REST based server access
* Introducing REST Typed Traverser with support for server side Prune Evaluator and Return Filter

#Using this library

##Graph Database Service Provider

Neo4j Scala Wrapper needs a Graph Database Service Provider, it has to implement GraphDatabaseServiceProvider trait.
One possibility is to use the EmbeddedGraphDatabaseServiceProvider for embedded Neo4j instances where you simply have to define a Neo4j storage directory.
The class MyNeo4jClass using the wrapper is f.e.:

```scala
class MyNeo4jClass extends SomethingClass with Neo4jWrapper with EmbeddedGraphDatabaseServiceProvider {
  def neo4jStoreDir = "/tmp/temp-neo-test"
  . . .
}
```

Available are:

* EmbeddedGraphDatabaseServiceProvider
* SingletonEmbeddedGraphDatabaseServiceProvider (singleton version)
* BatchGraphDatabaseServiceProvider (use it with Neo4jBatchIndexProvider)
* RestGraphDatabaseServiceProvider uses the REST binding

##Transaction Wrapping

Transactions are wrapped by withTx. After leaving the "scope" success is called (or rollback if an exception is raised):

```scala
withTx {
 implicit neo =>
   val start = createNode
   val end = createNode
   start --> "foo" --> end
}
```

##Using an Index

Neo4j provides indexes for nodes and relationships. The indexes can be configured by mixing in the Neo4jIndexProvider trait. See [Indexing](http://docs.neo4j.org/chunked/stable/indexing.html)

```scala
class MyNeo4jClass extends . . . with Neo4jIndexProvider {
  // configuration for the index being created.
  override def NodeIndexConfig = ("MyTest1stIndex", Some(Map("provider" -> "lucene", "type" -> "fulltext"))) ::
                                 ("MyTest2ndIndex", Some(Map("provider" -> "lucene", "type" -> "fulltext"))) :: Nil
}
```

Use one of the configured indexes with

```scala
val nodeIndex = getNodeIndex("MyTest1stIndex").get
```

Add and remove entries by:

```scala
nodeIndex += (Node_A, "title", "The Matrix")
nodeIndex -= (Node_A)
```

##Relations


Using this wrapper, this is how creating two relationships can look in Scala. 
The String are automatically converted into Dynamic Relationsships:

```scala
start --> "KNOWS" --> intermediary --> "KNOWS" --> end
left --> "foo" --> middle <-- "bar" <-- right
```

To return the Property Container for the Relation Object use the '<' method:

```scala
val relation = start --> "KNOWS" --> end <
```

##Properties

And this is how getting and setting properties on a node or relationship looks like :

```scala
// setting the property foo
start("foo") = "bar"
// cast Object to String and match . . .
start[String]("foo") match {
  case Some(x) => println(x)
  case None => println("aww")
}
```

##Using Case Classes

Neo4j provides storing keys (String) and values (Object) into Nodes. To store Case Classes the properties are stored as key/values to the Property Container, thai can be a Node or a Relation. However, Working types are limited to basic types like String, integer etc.

```scala
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
```

##Traversing

Besides, the neo4j scala binding makes it possible to write stop and returnable evaluators in a functional style :

```scala
//StopEvaluator.END_OF_GRAPH, written in a Scala idiomatic way :
start.traverse(Traverser.Order.BREADTH_FIRST, (tp : TraversalPosition) => false, 
ReturnableEvaluator.ALL_BUT_START_NODE, "foo", Direction.OUTGOING)

//ReturnableEvaluator.ALL_BUT_START_NODE, written in a Scala idiomatic way :
start.traverse(Traverser.Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, (tp : TraversalPosition) => tp.notStartNode(), 
"foo", Direction.OUTGOING)
```

##Typed Traversing

The traverser mentioned above processes and returns Nodes resp. Property Container. To allow a more type safe traverser the TypedTraverser was introduced. The basic semantic is that you have to define the type (a case class) that should be returned (while inheritance is respected).
But first define Relation Types and Directions:

###Relation Types and Direction

To define relation types and directions use the follow method. Some examples

```scala
follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY" // BOTH for "KNOWS", OUTGOING for "CODED_BY"
follow -<- "BAR" -- "FOO"                       // INCOMING for "BAR", BOTH for "FOO", defaults to DEPTH_FIRST
follow(DEPTH_FIRST) ->- "FOOBAR"                // OUTGOING for "FOOBAR"
```

So the traverser, that returns an Iterable[MatrixBase], can be written like this:

```scala
myNode.doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY") {
    . . .block1. . .
} {
    . . .block2. . .
}
```

###Return and Stop Evaluator

**block1** is the Stop Evaluator and **block2** the Return Evaluator, both of type PartialFunction[(T, TraversalPosition), Boolean]. Where T is MatrixBase in case of the example, TraversalPosition from the traverser and Boolean as return type. 

PartialFunctions can be handled with a case statement, like this:

```scala
node.doTraverse[MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY") {
   		END_OF_GRAPH // predefined partial function
 	} {
		// if node is of type Matrix and TraversalPosition.depth == 2 then check lenth
   		case (x: Matrix, tp) if (tp.depth == 2) => x.name.length > 2

		// if node is of type NonMatrix then false
   		case (y: NonMatrix, _) => false
 	}
```

Assuming that Matrix and NonMatrix are inherited from MatrixBase the traverser will handle that correctly. X and y only matches if the given type can be marshaled to Matrix or NonMatrix and assigned from MatrixBase. Additionally, you can use the TraversalPosition in both case statements or ignore it with '_'. The example defines that depth must be 2 and the Matrix.name parameter must be of length > 2. NonMatrix classes are not returned. The Stop Evaluator always returns false.

###Iterable[T]

The example above returns an Iterable[MatrixBase]. This allows to use the powerful Scala collections, f.e.:

```scala
...}.toList.sortWith(_.name < _.name) // sorts Nodes by name

...}.toList.flatMap(_.name) // List(N, e, o, M, o, r, p, h, e, u, s)

...}.toList.par. . . // do something with parallel collections
```

Where '_' is automatically replaced by MatrixBase instances.
	
Finally we can write, f.e.:

```scala
val list:List[MatrixBase] = node.doTraverse[MatrixBase](follow -<- "KNOWS") {
    case _ => false
} {
    case (x: Matrix, _) => true
    case (x: NonMatrix, _) => false
}.toList.sortWith(_.name < _.name)
```

###Using a List of Nodes

Instead of one Node you can use a List of Nodes (List[Node]). The given traverser is started multithreaded for every node in the List. The resulting threads are joined, the result-lists are appended and Node duplicates removed. F.e:

```scala
val erg1 = startWithNodes.doTraverse[MatrixBase](follow -<- "KNOWS") {
    case _ => false
  } {
    case (x: Matrix, _) => true
    case (x: NonMatrix, _) => false
  }.toList.sortWith(_.name < _.name)
```

Where startWithNodes is of type List[Node].


##REST Typed Traversing 

The main diffenrence between the non REST Typed Traverser is the ability to provide server side Prune Evaluator and Return Filter. This is important because otherwise all traversed data will be transfered to the client. This is possible but not always the best solution.


###Prune Evaluator and Max Depth

The **PruneEvaluator** defines where to stop traversing relations. It has to be Java Script code that can use the position instance of type org.neo4j.graphdb.Path.
 
**max depth** is a short-hand way of specifying a prune evaluator which prunes after a certain depth. If not specified

* a max depth of 1 is used and 
* if a "prune evaluator" is specified instead of a max depth, no max depth limit is set.

####Examples for Prune Evaluator / MaxDepth
Using the case class PruneEvaluator ("JAVASCRIPT" is dafault, "false" is Java Script code):
  
```scala    
startNode.doTraverse[Test_MatrixBase](follow -- "KNOWS" ->- "CODED_BY") {
  PruneEvaluator("false")
} {
  case (x: Test_Matrix, tp) if (tp.depth == 3) => x.name.length > 2
  case (x: Test_NonMatrix, _) => false
}.toList.sortWith(_.name < _.name)
```

Using a Java Script prune evaluator as a String (implicit conversion involved)
    
```scala  
startNode.doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY") {
  "position.length() > 100;"
} {
  case (x: Test_Matrix, tp) if (tp.depth == 2) => x.name.length > 2
  case (x: Test_NonMatrix, _) => false
}.toList.sortWith(_.name < _.name)
```
      
Using MaxDepth 100:

```scala
startNode.doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY")(100) {
  case (x: Test_Matrix, _) => x.name.length > 2
}.toList.sortWith(_.name < _.name)
```

###Return Filter

The Return Filter has the same semantic as the Return Evaluator. It can be used as with the normal TypedTraverser, can be used with the Java Script and with two builtin functions:

* ReturnAllButStartNode
* ReturnAll

####Examples for Return Filter
Traversing with Max Depth 10 and all nodes except start node:

```scala
startNode.
       doTraverse[…](follow(BREADTH_FIRST) -- "KNOWS", 10, ReturnAllButStartNode)
```

Using Java Script Prune Evaluator ("true"):

```scala
startNode.
       doTraverse[…](follow(BREADTH_FIRST) ->- "CODED_BY", 1, "true")
```
Server Side type check and Max Depth 3:

```scala
startNode.doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY", 3,
    endNode.isOfType[Test_Matrix]
  ).toList.sortWith(_.name < _.name)
```

Server Side type check and Server Side Java Script Prune Evaluator

```scala     
startNode.doTraverse[Test_MatrixBase](follow(BREADTH_FIRST) -- "KNOWS" ->- "CODED_BY",
    "position.length() >= 1",
    endNode.isOfType[Test_Matrix]
  ).toList.sortWith(_.name < _.name)
```

##Simple Cypher Support

The following example shows how to use Cypher together with typed results. In this case "execute" returns the case class Test_Matrix.

```scala
class MyClass extends Neo4jWrapper with SingletonEmbeddedGraphDatabaseServiceProvider with Cypher {
	. . .
	val query = "start n=node(" + nodeId + ") return n, n.name"

    val typedResult = query.execute.asCC[Test_Matrix]("n")

    typedResult.next.name must be_==("Neo")
    . . .
}
```


##Batch Processing

Neo4j has a batch insertion mode intended for initial imports, which must run in a single thread and bypasses transactions and other checks in favor of performance. See [Batch insertion](http://docs.neo4j.org/chunked/milestone/indexing-batchinsert.html).

The Java interfaces are slightly different. I wrote some wrapper classes to support nearly transparent usage of batch node and batch relation insertion. Means same code for batch insertion and for normal non batch mode. Instead of using

```scala
class Builder extends Neo4jWrapper with SingletonEmbeddedGraphDatabaseServiceProvider with Neo4jIndexProvider {...}
```

simply exchange the provider traits with

```scala
class Builder extends Neo4jWrapper with Neo4jBatchIndexProvider with BatchGraphDatabaseServiceProvider {...}
```

getting the indexes is still the same code

```scala
val nodeIndex = getNodeIndex("NodeIndex").get
val relationIndex = getRelationIndex("RelationIndex").get
```

setting cache size:

```scala
nodeIndex.setCacheCapacity("NodeIndex", 1000000)
relationIndex.setCacheCapacity("RelationIndex", 1000000)
```

Nevertheless, indexes are not available till flushing. To flush call:

```scala
nodeIndex.flush
relationIndex.flush
```

After insertion, the batch index manager and batch insertion manager have to be shut down

```scala
class Builder extends Neo4jWrapper . . .{
	. . .
	shutdownIndex
	shutdown(ds)
	. . .
}
```

Copyright and License
---------------------

Copyright (C) 2012 [Christopher Schmidt](http://blog.fakod.eu/) 


Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
