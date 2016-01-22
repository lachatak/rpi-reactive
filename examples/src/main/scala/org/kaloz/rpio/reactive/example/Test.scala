package org.kaloz.rpio.reactive.example

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.GpioBoard._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel
import rx.lang.scala.Observer
import rx.lang.scala.subjects.PublishSubject

import scalaz.Scalaz._
import scalaz._

object Test extends App with StrictLogging {

  val countDownLatch = new CountDownLatch(1)

  implicit val protocolHandlerFactory = PiGpioSocketChannel
  implicit val subject = PublishSubject[Event]()
  subject.subscribe(Observer[Event]((event: Event) => println(event)))

  val state = scalaz.StateT.stateMonad[GpioBoard]

  def initMotor(): State[GpioBoard, Unit] = for {
    _ <- provisionGpioInputPin(17, PudMode.PudUp)
    _ <- provisionDefaultGpioOutputPins(25, 16, 21)
    _ <- provisionGpioPwmOutputPin(12)
    board <- get[GpioBoard]
    _ <- subscribeOneOffPinValueChangedEvent(17, Falling_Edge, _ => startMotor run (board))
  } yield {}

  def startMotor(): State[GpioBoard, Unit] = for {
    _ <- writeValue(25, High)
    _ <- writeValue(16, High)
    _ <- writeValue(12, Pwm(100))
    board <- get[GpioBoard]
    _ <- subscribeOneOffPinValueChangedEvent(17, Falling_Edge, _ => stopMotor run (board))
  } yield {}

  def stopMotor(): State[GpioBoard, Unit] = for {
    _ <- shutdown()
  } yield {
    countDownLatch.countDown()
  }

  initMotor run (new GpioBoard())

  countDownLatch.await()
}
