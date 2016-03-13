package org.kaloz.rpio.reactive.example

object Test2 extends App {

  sealed trait Base

  case class A() extends Base

  case class B() extends Base

//  def test(t: A) {
//    println("1")
//  }

  def test(t: B) {

    println("2")
  }

  val a:Base = A()

  test(a)
}
