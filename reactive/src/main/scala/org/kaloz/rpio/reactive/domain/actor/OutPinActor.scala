package org.kaloz.rpio.reactive.domain.actor

import akka.actor.{Actor, Props}
import org.kaloz.rpio.reactive.domain.GpioPin.GpioOutputPin
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.api._
//import org.kaloz.rpio.reactive.domain.service.PinManipulationService._

//object OutPinActor {
//  def props(pinNumber: Int)(implicit sendReceiveHandler: SendReceiveHandler, domainPublisher: DomainPublisher): Props = Props(new OutPinActor(pinNumber))
//}
//
//class OutPinActor(pinNumber: Int)(implicit sendReceiveHandler: SendReceiveHandler, domainPublisher: DomainPublisher) extends Actor {
//
//  val gpioOutputPin = GpioOutputPin(pinNumber)
//
//  def receive: Receive = {
//    case newValue: PinValue => {
//      writeValue(gpioOutputPin, newValue)
//    }
//  }
//}
