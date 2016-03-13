package org.kaloz.rpio.reactive.example

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.GpioBoard
import org.kaloz.rpio.reactive.domain.GpioBoard._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import org.kaloz.rpio.reactive.domain.service.DomainPublisher
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel

import scalaz._

object Test extends App with StrictLogging {

  val countDownLatch = new CountDownLatch(1)

  implicit val sendReceiveHandler = PiGpioSocketChannel
  implicit val domainPublisher = DomainPublisher

  domainPublisher.subscribe {
    case x => println(x)
  }

  def initMotor(): GpioBoardState = for {
    _ <- provisionGpioInputPin(17, PudUp)
    _ <- provisionDefaultGpioOutputPins(25, 16, 21)
    _ <- provisionGpioPwmOutputPin(12)
    value <- readValue(12)
    _ <- subscribeOneOffPinValueChangedEvent(17, Falling_Edge, startMotor)
  } yield {
    println(value)
  }

  def startMotor(): GpioBoardState = for {
    _ <- writeValue(25, High)
    _ <- writeValue(16, High)
    _ <- writeValue(12, Pwm(100))
    _ <- subscribeOneOffPinValueChangedEvent(17, Falling_Edge, stopMotor)
  } yield {}

  def stopMotor(): GpioBoardState = for {
    value <- readValue(17)
    _ <- shutdown()
  } yield {
    println(value)
    countDownLatch.countDown()
  }

  initMotor run (new GpioBoard())

  countDownLatch.await()
}
