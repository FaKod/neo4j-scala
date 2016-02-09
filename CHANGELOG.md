##0.3.2-SNAPSHOT
* Thanks to Ouven switched to SBT
* Bumped versions to 2.2.2
* prepared for cross builds -> ArtifactID now neo4j-scala_2.10
* but, Cypher does not work with Scala 2.11 and removed the discontinued REST support

##0.3.1-SNAPSHOT
* Thanks to Alexander Korneev: fixes such warnings by replacing implicit methods that return structural types, with implicit classes. It also fixes some other warnings that arise when compiling the library itself.
* Thanks to Stephen Muss: Bumped Version to 2.1.3 and added support for nodes with labels. Unfortunately, Neo4j mirrored class org.neo4j.rest.graphdb.traversal.WrappingResourceIterator in Neo4js Kernel package (neo4j-contrib/java-rest-binding#70). Thats why I got a a IllegalAccessError. As a **workaround** I placed the neo4j-rest-graphdb dependency before the one of the neo4j-kernel.

##0.3.0
* Based on 0.2.0-M3-SNAPSHOT
* **Refactoring from org.neo4j.scala to eu.fakod.neo4jscala**.
* "Old"" version is still in the 0.2.0-M git branch
* using artifact id **neo4j-scala_2.10** now
* deployed to central
* I had to refactor the packages to my own domain, to allow deployment to Sonatype Maven repository.
* Snapshots aren't synchronized with Central. So use Sonatype repo for now (see below)
* "old" version is still in the 0.2.0-M git branch.
* further developments in master branch

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

