package org.kaloz.rpio.reactive.example

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel
import rx.lang.scala.Observer
import rx.lang.scala.subjects.PublishSubject

import scalaz._

object Test extends App with StrictLogging {

  implicit val protocolHandlerFactory = PiGpioSocketChannel
  implicit val subject = PublishSubject[Event]()

  //    val subscription = subject.filter(_ == PinValueChangedEvent(25, Falling_Edge, Low)).subscribe(Observer[Event]((event: Event) => println(event)))
  val subscription = subject.subscribe(Observer[Event]((event: Event) => println(event)))

  val state = scalaz.StateT.stateMonad[GpioBoard]

  import GpioBoard._

  def startMotor(): State[GpioBoard, Unit] = for {
    _ <- provisionDefaultGpioOutputPins(25, 16, 21)
    _ <- provisionGpioPwmOutputPin(12)
    _ <- writeValue(25, High)
    _ <- writeValue(16, High)
    _ <- writeValue(12, Pwm(100))
  } yield {}

  println("Start..")
  val (board, _) = startMotor run (new GpioBoard())
  println(s"State after start: $board")

  Thread.sleep(5000)

  def stopMotor(): State[GpioBoard, Unit] = for {
    _ <- shutdown()
  } yield {}

  val (board2, _) = stopMotor run (board)
  println(s"State after stop: $board2")
}
