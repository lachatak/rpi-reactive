package org.kaloz.rpio.reactive.domain

import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.duration._
import scala.reflect.ClassTag
import scalaz.Scalaz._
import scalaz._

sealed abstract class GpioPin(val pinNumber: Int, mode: PinMode, closed: Boolean)(implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) {

  type Self <: GpioPin

  protected def changePinMode(pinMode: PinMode): ChangePinModeResponse =
    protocolHandler.request(ChangePinModeRequest(pinNumber, pinMode)).asInstanceOf[ChangePinModeResponse]

  def pinMode(): PinMode = mode

  def readValue(): PinValue

  def close(): Self

}

object GpioOutputPin {
  def create(pinNumber: Int,
             value: PinValue = PinValue.Low,
             default: PinValue = PinValue.Low,
             closed: Boolean = false)
            (implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]): GpioOutputPin = {

    implicit val protocolHandler = protocolHandlerFactory(pinNumber.some)

    val newPin = new GpioOutputPin(pinNumber, value, default, closed)

    newPin.changePinMode(PinMode.Output)
    newPin.writeValue(value)

    newPin
  }
}

case class GpioOutputPin(override val pinNumber: Int,
                         value: PinValue,
                         default: PinValue,
                         closed: Boolean)
                        (implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) extends GpioPin(pinNumber, PinMode.Output, closed) {

  type Self = GpioOutputPin

  def readValue(): PinValue = value

  def writeValue(newValue: PinValue): GpioOutputPin = {
    protocolHandler.request(WriteValueRequest(pinNumber, newValue)).asInstanceOf[WriteValueResponse]
    import PinValue.pinValueToInt
    (value, newValue) match {
      case (o, n) if (o == n) => this
      case (o, n) if (o < n) =>
        subject.onNext(PinValueChangedEvent(pinNumber, Rising_Edge, newValue))
        this.copy(value = newValue)
      case (o, n) if (o > n) =>
        subject.onNext(PinValueChangedEvent(pinNumber, Falling_Edge, newValue))
        this.copy(value = newValue)
    }
  }

  def close() = {
    writeValue(default)
    protocolHandler.close()
    subject.onNext(PinClosedEvent(pinNumber))
    copy(value = default, closed = true)
  }

}

object GpioInputPin {
  def create(pinNumber: Int,
             pudMode: PudMode = PudMode.PudDown,
             closed: Boolean = false)
            (implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]): GpioInputPin = {

    implicit val protocolHandler = protocolHandlerFactory(pinNumber.some)

    val newPin = new GpioInputPin(pinNumber, pudMode, closed)

    newPin.changePinMode(PinMode.Input)
    newPin.changePudMode(pudMode)
    newPin.changeListener()

    newPin
  }
}

case class GpioInputPin(override val pinNumber: Int,
                        pud: PudMode = PudDown,
                        closed: Boolean = false)
                       (implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) extends GpioPin(pinNumber, PinMode.Input, closed) with Configuration {

  type Self = GpioInputPin

  private def changePudMode(pudMode: PudMode): ChangePudModeResponse =
    protocolHandler.request(ChangePudModeRequest(pinNumber, pudMode)).asInstanceOf[ChangePudModeResponse]

  def pudMode() = pud

  def readValue(): PinValue = protocolHandler.request(ReadValueRequest(pinNumber)).asInstanceOf[ReadValueResponse].value

  def close() = {
    protocolHandler.close()
    subject.onNext(PinClosedEvent(pinNumber))
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
          subject.onNext(PinValueChangedEvent(pinNumber, Rising_Edge, currentValue))
        case (o, n) if (o > n) =>
          subject.onNext(PinValueChangedEvent(pinNumber, Falling_Edge, currentValue))
      }
      lastValue = currentValue
    })
  }
}

object GpioBoard {

  def provisionGpioOutputPin(pinNumber: Int,
                             value: PinValue = PinValue.Low,
                             default: PinValue = PinValue.Low) = modify[GpioBoard] { gpioBoard =>
    gpioBoard.provisionGpioOutputPin(pinNumber, value, default)
  }

  def provisionGpioPwmOutputPin(pinNumber: Int,
                                value: PinValue = Pwm(0),
                                default: PinValue = Pwm(0)) = modify[GpioBoard] { gpioBoard =>
    gpioBoard.provisionGpioOutputPin(pinNumber, value, default)
  }

  def provisionDefaultGpioOutputPins(pinNumbers: Int*) = modify[GpioBoard] { gpioBoard =>
    gpioBoard.provisionDefaultGpioPins[GpioOutputPin](pinNumbers: _*)
  }

  def provisionGpioInputPin(pinNumber: Int, pudMode: PudMode = PudMode.PudDown) = modify[GpioBoard] { gpioBoard =>
    gpioBoard.provisionGpioInputPin(pinNumber, pudMode)
  }

  def provisionDefaultGpioInputPins(pinNumbers: Int*) = modify[GpioBoard] { gpioBoard =>
    gpioBoard.provisionDefaultGpioPins[GpioInputPin](pinNumbers: _*)
  }

  def writeValue(pinNumber: Int, newValue: PinValue) = modify[GpioBoard] { gpioBoard =>
    gpioBoard.writeValue(pinNumber, newValue)
  }

  def shutdown() = modify[GpioBoard] { gpioBoard =>
    gpioBoard.shutdown()
  }
}

case class GpioBoard(pins: Map[Int, GpioPin] = Map.empty, pwmCapablePins: Set[Int] = Set(12))(implicit protocolHandlerFactory: ProtocolHandlerFactory, subject: Subject[Event]) {

  def provisionGpioOutputPin(pinNumber: Int,
                             value: PinValue = PinValue.Low,
                             default: PinValue = PinValue.Low): GpioBoard =
    pins.values.find(_.pinNumber == pinNumber) match {
      case None =>
        val newPin = GpioOutputPin.create(pinNumber, value, default)
        subject.onNext(PinProvisionedEvent(pinNumber, PinMode.Output))
        copy(pins = pins + (pinNumber -> newPin))
      case Some(pin) if pin.pinMode() == PinMode.Output => this
      case Some(pin) => throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode()}!!")
    }

  def provisionGpioInputPin(pinNumber: Int,
                            pudMode: PudMode = PudMode.PudDown): GpioBoard =
    pins.values.find(_.pinNumber == pinNumber) match {
      case None =>
        val newPin = GpioInputPin.create(pinNumber, pudMode)
        subject.onNext(PinProvisionedEvent(pinNumber, PinMode.Output))
        copy(pins = pins + (pinNumber -> newPin))
      case Some(pin) if pin.pinMode() == PinMode.Output => this
      case Some(pin) => throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode()}!!")
    }

  def provisionDefaultGpioPins[A: ClassTag](pinNumbers: Int*): GpioBoard = {
    val gpiooutputpin = implicitly[ClassTag[GpioOutputPin]]
    val gpioinputpin = implicitly[ClassTag[GpioInputPin]]

    pinNumbers.foldLeft[GpioBoard](this)((currentBoard, currentPinNumber) =>
      implicitly[ClassTag[A]] match {
        case `gpiooutputpin` => currentBoard.provisionGpioOutputPin(currentPinNumber)
        case `gpioinputpin` => currentBoard.provisionGpioInputPin(currentPinNumber)
      }
    )
  }

  def unprovisionGpioPin(pinNumber: Int): GpioBoard =
    pins.values.find(_.pinNumber == pinNumber) match {
      case None => this
      case Some(pin) => copy(pins = pins - pinNumber)
    }

  def writeValue(pinNumber: Int, newValue: PinValue): GpioBoard = {
    pins.values.find(_.pinNumber == pinNumber) match {
      case None => throw new IllegalArgumentException(s"Pin $pinNumber is not initialised!!")
      case Some(pin) if pin.pinMode() == PinMode.Output =>
        copy(pins = pins + (pin.pinNumber -> pin.asInstanceOf[GpioOutputPin].writeValue(newValue)))
      case Some(pin) => throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode()}!!")
    }
  }

  def shutdown(): GpioBoard = {
    val newBoard = copy(pins = pins.mapValues(_.close()))
    subject.onNext(GpioBoardShutDownEvent())
    newBoard
  }

}
