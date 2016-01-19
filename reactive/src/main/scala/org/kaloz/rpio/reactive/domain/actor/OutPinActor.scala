package org.kaloz.rpio.reactive.domain.actor

import akka.actor.{Actor, Props}
import org.kaloz.rpio.reactive.domain.DomainApi.{Event, ProtocolHandlerFactory}
import org.kaloz.rpio.reactive.domain.GpioOutputPin
import org.kaloz.rpio.reactive.domain.PinValue._
import rx.lang.scala.Subject

object OutPinActor {
  def props(pinNumber: Int)(implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]): Props = Props(new OutPinActor(pinNumber))

}

class OutPinActor(pinNumber: Int)(implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]) extends Actor {

  val gpioOutputPin = GpioOutputPin(pinNumber)

  def receive: Receive = {
    case x: PinValue => {
      gpioOutputPin.writeValue(x)
    }
  }
}
