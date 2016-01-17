package org.kaloz.rpio.reactive.domain

import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import rx.lang.scala.subjects._
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.Scalaz._

sealed abstract class GpioPin(val pin: Int, mode: PinMode, closed: Boolean)(implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) {

  type Self <: GpioPin

  protected def changePinMode(pinMode: PinMode): ChangePinModeResponse =
    protocolHandler.request(ChangePinModeRequest(pin, pinMode)).asInstanceOf[ChangePinModeResponse]

  def pinMode(): PinMode = mode

  def readValue(): PinValue

  def close(): Self

}

object GpioOutputPin {
  def create(pin: Int,
             value: PinValue = PinValue.Low,
             default: PinValue = PinValue.Low,
             closed: Boolean = false)
            (implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]): GpioOutputPin = {

    implicit val protocolHandler = protocolHandlerFactory(pin.some)

    val newPin = new GpioOutputPin(pin, value, default, closed)

    newPin.changePinMode(PinMode.Output)
    newPin.writeValue(value)

    newPin
  }
}

case class GpioOutputPin(override val pin: Int,
                         value: PinValue,
                         default: PinValue,
                         closed: Boolean)
                        (implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) extends GpioPin(pin, PinMode.Output, closed) {

  type Self = GpioOutputPin

  def readValue(): PinValue = value

  def writeValue(newValue: PinValue): GpioOutputPin = {
    protocolHandler.request(WriteValueRequest(pin, newValue)).asInstanceOf[WriteValueResponse]
    import PinValue.pinValueToInt
    (value, newValue) match {
      case (o, n) if (o == n) => this
      case (o, n) if (o < n) =>
        subject.onNext(PinValueChangedEvent(pin, Rising_Edge, newValue))
        this.copy(value = newValue)
      case (o, n) if (o > n) =>
        subject.onNext(PinValueChangedEvent(pin, Falling_Edge, newValue))
        this.copy(value = newValue)
    }
  }

  def close() = {
    writeValue(default)
    protocolHandler.close()
    subject.onNext(PinClosedEvent(pin))
    copy(value = default, closed = true)
  }

}

object GpioInputPin {
  def create(pin: Int,
             pudMode: PudMode = PudMode.PudDown,
             closed: Boolean = false)
            (implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]): GpioInputPin = {

    implicit val protocolHandler = protocolHandlerFactory(pin.some)

    val newPin = new GpioInputPin(pin, pudMode, closed)

    newPin.changePinMode(PinMode.Input)
    newPin.changePudMode(pudMode)
    newPin.changeListener()

    newPin
  }
}

case class GpioInputPin(override val pin: Int,
                        pud: PudMode = PudDown,
                        closed: Boolean = false)
                       (implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) extends GpioPin(pin, PinMode.Input, closed) with Configuration {

  type Self = GpioInputPin

  private def changePudMode(pudMode: PudMode): ChangePudModeResponse =
    protocolHandler.request(ChangePudModeRequest(pin, pudMode)).asInstanceOf[ChangePudModeResponse]

  def pudMode() = pud

  def readValue(): PinValue = protocolHandler.request(ReadValueRequest(pin)).asInstanceOf[ReadValueResponse].value

  def close() = {
    protocolHandler.close()
    subject.onNext(PinClosedEvent(pin))
    copy(closed = true)
  }

  def changeListener(): Unit = {
    var lastValue = readValue()
    val intervals = Observable.interval(observablePin.refreshInterval millisecond)
    intervals.subscribe(next => {
      val currentValue = readValue()
      import PinValue.pinValueToInt
      (lastValue, currentValue) match {
        case (o, n) if (o == n) =>
        case (o, n) if (o < n) =>
          subject.onNext(PinValueChangedEvent(pin, Rising_Edge, currentValue))
        case (o, n) if (o > n) =>
          subject.onNext(PinValueChangedEvent(pin, Falling_Edge, currentValue))
      }
      lastValue = currentValue
    })
  }
}
