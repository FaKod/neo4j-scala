package org.neo4j.scala

trait Test_MatrixBase {
  val name: String
  val profession: String
}

case class Test_Matrix(name: String, profession: String) extends Test_MatrixBase

case class Test_NonMatrix(name: String, profession: String) extends Test_MatrixBase
