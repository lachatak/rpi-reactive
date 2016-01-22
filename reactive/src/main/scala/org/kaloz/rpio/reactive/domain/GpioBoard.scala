package org.kaloz.rpio.reactive.domain

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinValue.{PinValue, Pwm}
import org.kaloz.rpio.reactive.domain.PudMode.PudMode
import rx.lang.scala.schedulers.NewThreadScheduler
import rx.lang.scala.{Observer, Subject, Subscription}

import scala.reflect.ClassTag
import scalaz.Scalaz._

case class GpioBoard(pins: Map[Int, GpioPin] = Map.empty, pwmCapablePins: Set[Int] = Set(12, 13, 18, 19))(implicit protocolHandlerFactory: ProtocolHandlerFactory, val subject: Subject[Event]) extends StrictLogging {

  def provisionGpioOutputPin(pinNumber: Int,
                             value: PinValue = PinValue.Low,
                             default: PinValue = PinValue.Low): GpioBoard =
    applyOnPin[GpioBoard](pinNumber,
      empty = () => {
        (value, default) match {
          case (Pwm(_), _) | (_, Pwm(_)) if (!pwmCapablePins.contains(pinNumber)) => throw new IllegalArgumentException(s"Pin $pinNumber cannot be initialised as a PWM pin!")
          case (_, _) =>
        }
        val newPin = GpioOutputPin(pinNumber, value, default)
        subject.onNext(PinProvisionedEvent(pinNumber, PinMode.Output))
        copy(pins = pins + (pinNumber -> newPin))
      },
      nonEmpty = pin =>
        (pin.pinMode == PinMode.Output).fold(
          this,
          throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode}!!")
        )
    )

  def provisionGpioInputPin(pinNumber: Int,
                            pudMode: PudMode = PudMode.PudDown): GpioBoard =
    applyOnPin[GpioBoard](pinNumber,
      empty = () => {
        val newPin = GpioInputPin(pinNumber, pudMode)
        subject.onNext(PinProvisionedEvent(pinNumber, PinMode.Input))
        copy(pins = pins + (pinNumber -> newPin))
      },
      nonEmpty = pin =>
        (pin.pinMode == PinMode.Input).fold(
          this,
          throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode}!!")
        )
    )

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
    applyOnPin[GpioBoard](pinNumber,
      empty = () => this,
      nonEmpty = pin => copy(pins = pins - pinNumber)
    )

  def writeValue(pinNumber: Int, newValue: PinValue): GpioBoard =
    applyOnPin[GpioBoard](pinNumber,
      empty = () => throw new IllegalArgumentException(s"Pin $pinNumber is not initialised!!"),
      nonEmpty = pin =>
        (pin.pinMode == PinMode.Output).fold(
          copy(pins = pins + (pin.pinNumber -> pin.asInstanceOf[GpioOutputPin].writeValue(newValue))),
          throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode}!!")
        )
    )

  def readValue(pinNumber: Int): Option[PinValue] =
    applyOnPin[Option[PinValue]](pinNumber,
      empty = () => None,
      nonEmpty = pin => pin.value.some)

  def shutdown(): GpioBoard = {
    val newBoard = copy(pins = pins.mapValues(_.close()).map(identity))
    subject.onNext(GpioBoardShutDownEvent())
    newBoard
  }

  private def applyOnPin[A](pinNumber: Int, empty: () => A, nonEmpty: GpioPin => A): A =
    pins.values.find(_.pinNumber == pinNumber) match {
      case None => empty()
      case Some(pin) => nonEmpty(pin)
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

  def subscribeOneOffPinValueChangedEvent(pinNumber: Int, direction: Direction, eventOn: Event => Unit) = gets[GpioBoard, Subscription] { gpioBoard =>
    gpioBoard.subject.filter(_ match {
      case PinValueChangedEvent(`pinNumber`, `direction`, _) => true
      case _ => false
    }).take(1).observeOn(NewThreadScheduler()).subscribe(Observer[Event]((event: Event) => eventOn(event)))
  }

  def writeValue(pinNumber: Int, newValue: PinValue) = modify[GpioBoard] { gpioBoard =>
    gpioBoard.writeValue(pinNumber, newValue)
  }

  def shutdown() = modify[GpioBoard] { gpioBoard =>
    gpioBoard.shutdown()
  }
}
