package org.kaloz.rpio.reactive.example

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain._
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.GpioOutputPin
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel
import rx.lang.scala.Observer
import rx.lang.scala.subjects.PublishSubject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import scalaz.Scalaz._
import scalaz._

object Test extends App with StrictLogging {

  implicit val protocolHandlerFactory = PiGpioSocketChannel
  implicit val subject = PublishSubject[Event]()

  val createdPins = Seq(GpioOutputPin.create(pin = 25), GpioOutputPin.create(pin = 16), GpioOutputPin.create(pin = 21), GpioOutputPin.create(pin = 12, value = Pwm(0), default = Pwm(0))).map(pin => (pin.pin -> pin)).toMap

  //    val subscription = subject.filter(_ == PinValueChangedEvent(25, Falling_Edge, Low)).subscribe(Observer[Event]((event: Event) => println(event)))
  val subscription = subject.subscribe(Observer[Event]((event: Event) => println(event)))

  case class PinController(pins: Map[Int, GpioPin] = Map.empty) {

    def updatePin(pin: GpioPin): PinController = copy(pins = pins - pin.pin).copy(pins = pins + (pin.pin -> pin))

    def pin[A <: GpioPin](pin: Int): A = pins.get(pin).get.asInstanceOf[A]

    def close(): PinController = copy(pins = pins.mapValues(_.close()))
  }

  val state = scalaz.StateT.stateMonad[PinController]

  def simulateMachine(): State[PinController, Unit] = for {
    a <- init[PinController]
    _ <- modify[PinController](pinController => pinController.updatePin(pinController.pin[GpioOutputPin](25).writeValue(High)))
    _ <- modify[PinController](pinController => pinController.updatePin(pinController.pin[GpioOutputPin](16).writeValue(High)))
    _ <- modify[PinController](pinController => pinController.updatePin(pinController.pin[GpioOutputPin](12).writeValue(Pwm(100))))
    _ <- modify[PinController](pinController => {
      Thread.sleep(5000)
      pinController
    })
    _ <- modify[PinController](pinController => pinController.close())
    f <- get[PinController]
  } yield {
//    println(a)
//    f
  }

  val (s, _) = simulateMachine() run (PinController(createdPins))
  println("State: " + s)

}
