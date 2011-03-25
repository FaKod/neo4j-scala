Neo4j Spatial Scala wrapper library
=======================

I tried to add some wrapper stuff for the Neo4j Spatial implementation. 
So you are able to create a city Munich as follows:

    val munich = add newPoint ((15.3, 56.2))
    munich.setProperty("City", "Munich")

and attached it to a federal state like Bavaria:

    val bayernBuffer = Buffer[(Double, Double)]((15, 56), (16, 56), (15, 57), (16, 57), (15, 56))
    val bayern = add newPolygon (LinRing(bayernBuffer))
    bayern.setProperty("FederalState", "Bayern")
    federalStates --> "isFederalState" --> bayern

Additionally I added some examples like those pattern shown in the [Neo4j Design Guide](http://wiki.neo4j.org/content/Design_Guide):

    . . .
	class FedaralState(val node: SpatialDatabaseRecord) extends . . . {

	  object FedaralState {
	    val KEY_FEDSTATE_NAME = "federalState"
	  }

	  def name = node.getProperty(FedaralState.KEY_FEDSTATE_NAME)

	  def name_=(n: String) {
	    node.setProperty(FedaralState.KEY_FEDSTATE_NAME, n)
	  }

	  def getCapitalCity(implicit layer: EditableLayer) = {
	    val o = node.getSingleRelationship("CapitalCityOf", Direction.INCOMING).getOtherNode(node)
	    new City(new SpatialDatabaseRecord(layer, o))
	  }
	}
	. . .
	
that finaly result in code as follows:

     /**
      * create Munich and "attach" it to the cities node
      */
     val munich = NewSpatialNode[City]((15.3, 56.2))
     munich.name = "Munich"
     cities --> "isCity" --> munich

     /**
      * create a polygon called Bayern, "attach" it to the federal state node and
      * "attach" the capital city Munich
      */
     val bayernBuffer = Buffer[(Double, Double)]((15, 56), (16, 56), (15, 57), (16, 57), (15, 56))
     val bayern = NewSpatialNode[FedaralState](bayernBuffer)
     bayern.name = "Bayern"
     federalStates --> "isFederalState" --> bayern
     munich --> "CapitalCityOf" --> bayern
	
Lookes rather nice IMHO, but is still very incomplete...

Neo4j Scala wrapper library
=======================

The Neo4j Scala wrapper library allows you the [Neo4j open source graph database](http://neo4j.org/) through a
domain-specific simplified language. It is written in Scala and is intended
to be used in other Scala projects.

This wrapper is mostly based on the work done by [Martin Kleppmann](http://twitter.com/martinkl) in his [Scala implementation of RESTful JSON HTTP resources on top of the Neo4j graph database and Jersey](http://github.com/ept/neo4j-resources) project. I thought it'd be usefull to extract the Neo4j DSL into a seperate project, and Marting agreed to this.


Building
--------

You need a Java 5 (or newer) environment and Maven 2.0.9 (or newer) installed:

    $ mvn --version
    Apache Maven 3.0-alpha-5 (r883378; 2009-11-23 16:53:41+0100)
    Java version: 1.6.0_15
    Java home: /usr/lib/jvm/java-6-sun-1.6.0.15/jre
    Default locale: en_US, platform encoding: UTF-8
    OS name: "linux" version: "2.6.31-12-generic" arch: "i386" Family: "unix"

You should now be able to do a full build of `neo4j-resources`:

    $ git clone git://github.com/jawher/neo4j-scala.git
    $ cd neo4j-scala
    $ mvn clean install

To use this library in your projects, add the following to the `dependencies` section of your
`pom.xml`:

    <dependency>
      <groupId>org.neo4j</groupId>
      <artifactId>neo4j-scala</artifactId>
      <version>0.9.9-SNAPSHOT</version>
    </dependency>

If you don't use Maven, take `target/neo4j-scala-0.9.9-SNAPSHOT.jar` and all of its dependencies, and add them to your classpath.


Troubleshooting
---------------

Please consider using [Github issues tracker](http://github.com/jawher/neo4j-scala/issues) to submit bug reports or feature requests.


Using this library
------------------

Using this wrapper, this is how creating two relationships can look in Scala:

    start --> "KNOWS" --> intermediary --> "KNOWS" --> end

And this is how getting and setting properties on a node or relationship looks like :

    start("foo") = "bar"
    start("foo") match {
    	case Some(x) => println(x)
	case None => println("aww")
    }

Besides, the neo4j scala binding makes it possible to write stop and returnable evaluators in a functional style :

    //StopEvaluator.END_OF_GRAPH, written in a Scala idiomatic way :
    start.traverse(Traverser.Order.BREADTH_FIRST, (tp : TraversalPosition) => false, ReturnableEvaluator.ALL_BUT_START_NODE, DynamicRelationshipType.withName("foo"), Direction.OUTGOING)
    
    //ReturnableEvaluator.ALL_BUT_START_NODE, written in a Scala idiomatic way :
    start.traverse(Traverser.Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, (tp : TraversalPosition) => tp.notStartNode(), DynamicRelationshipType.withName("foo"), Direction.OUTGOING)


License
-------

See `LICENSE` for details.

