package org.kaloz.rpio.reactive.domain

import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import rx.lang.scala.{Subscription, Observable, Subject}

import scala.concurrent.duration._
import scalaz.Scalaz._

sealed abstract class GpioPin(val pinNumber: Int, val pinMode: PinMode, closed: Boolean)(implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) {

  type Self <: GpioPin

  protected def changePinMode(pinMode: PinMode): this.type = {
    protocolHandler.request(ChangePinModeRequest(pinNumber, pinMode))
    this
  }

  def value: PinValue

  def close(): Self

}

object GpioOutputPin {
  def apply(pinNumber: Int,
            value: PinValue = PinValue.Low,
            defaultValue: PinValue = PinValue.Low)
           (implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]): GpioOutputPin = {

    implicit val protocolHandler = protocolHandlerFactory(pinNumber.some)

    new GpioOutputPin(pinNumber, value, defaultValue)
  }
}

case class GpioOutputPin private(override val pinNumber: Int,
                                 value: PinValue,
                                 defaultValue: PinValue,
                                 closed: Boolean)
                                (implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) extends GpioPin(pinNumber, PinMode.Output, closed) {

  private def this(pinNumber: Int, value: PinValue, default: PinValue)(implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) {
    this(pinNumber, value, default, false)
    changePinMode(PinMode.Output).writeValue(value)
  }

  type Self = GpioOutputPin

  def writeValue(newValue: PinValue): GpioOutputPin = {
    protocolHandler.request(WriteValueRequest(pinNumber, newValue))
    import PinValue.pinValueToInt
    (value, newValue) match {
      case (o, n) if (o == n) => this
      case (o, n) if (o < n) =>
        subject.onNext(PinValueChangedEvent(pinNumber, Rising_Edge, newValue))
        copy(value = newValue)
      case (o, n) if (o > n) =>
        subject.onNext(PinValueChangedEvent(pinNumber, Falling_Edge, newValue))
        copy(value = newValue)
    }
  }

  def close() = {
    writeValue(defaultValue)
    protocolHandler.close()
    subject.onNext(PinClosedEvent(pinNumber))
    copy(value = defaultValue, closed = true)
  }

}

object GpioInputPin {
  def apply(pinNumber: Int,
            pudMode: PudMode = PudMode.PudDown)
           (implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]): GpioInputPin = {

    implicit val protocolHandler = protocolHandlerFactory(pinNumber.some)

    new GpioInputPin(pinNumber, pudMode).initChangeListener()
  }
}

case class GpioInputPin private(override val pinNumber: Int,
                                pudMode: PudMode,
                                closed: Boolean,
                                subscription: Option[Subscription])
                               (implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) extends GpioPin(pinNumber, PinMode.Input, closed) with Configuration {

  type Self = GpioInputPin

  private def this(pinNumber: Int, pudMode: PudMode)(implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) {
    this(pinNumber, pudMode, false, None)
    changePinMode(PinMode.Input).changePudMode(pudMode)
  }

  private def changePudMode(pudMode: PudMode): this.type = {
    protocolHandler.request(ChangePudModeRequest(pinNumber, pudMode))
    this
  }

  def value: PinValue = protocolHandler.request(ReadValueRequest(pinNumber)).asInstanceOf[ReadValueResponse].value

  def close() = {
    protocolHandler.close()
    subject.onNext(PinClosedEvent(pinNumber))
    subscription.foreach(_.unsubscribe())
    copy(closed = true, subscription = None)
  }

  def initChangeListener(): GpioInputPin = {
    var lastValue = value
    val intervals = Observable.interval(observablePin.refreshInterval millisecond)
    val subscription = intervals.subscribe(next => {
      val currentValue = value
      import PinValue.pinValueToInt
      (lastValue, currentValue) match {
        case (o, n) if (o == n) =>
        case (o, n) if (o < n) =>
          subject.onNext(PinValueChangedEvent(pinNumber, Rising_Edge, currentValue))
        case (o, n) if (o > n) =>
          subject.onNext(PinValueChangedEvent(pinNumber, Falling_Edge, currentValue))
      }
      lastValue = currentValue
    })
    copy(subscription = subscription.some)
  }
}
