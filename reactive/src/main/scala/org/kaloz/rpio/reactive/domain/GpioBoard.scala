package org.kaloz.rpio.reactive.domain

//import com.typesafe.scalalogging.StrictLogging
//import org.kaloz.rpio.reactive.config.Configuration
//import org.kaloz.rpio.reactive.domain.Direction._
//import org.kaloz.rpio.reactive.domain.GpioPin._
//import org.kaloz.rpio.reactive.domain.PinValue.{PinValue, Pwm}
//import org.kaloz.rpio.reactive.domain.PudMode._
//import org.kaloz.rpio.reactive.domain.api._
//import org.kaloz.rpio.reactive.domain.service.PinManipulationService._
//
//import scala.reflect.ClassTag
//import scalaz.Scalaz._
//import scalaz._
//
//case class GpioBoard(pins: Map[Int, GpioPin] = Map.empty, pwmCapablePins: Set[Int] = Set(12, 13, 18, 19))(implicit sendReceiveHandler: SendReceiveHandler, val domainPublisher: DomainPublisher) extends Configuration with StrictLogging {
//
//  def provisionGpioOutputPin(pinNumber: Int, value: PinValue = PinValue.Low, default: PinValue = PinValue.Low): GpioBoard =
//    applyOnPin[GpioBoard](pinNumber,
//      empty = () => {
//        (value, default) match {
//          case (Pwm(_), _) | (_, Pwm(_)) if (!pwmCapablePins.contains(pinNumber)) => throw new IllegalArgumentException(s"Pin $pinNumber cannot be initialised as a PWM pin!")
//          case (_, _) =>
//        }
//        outputPin(pinNumber, value, default)(sendReceiveHandler) match {
//          case \/-(newPin) =>
//            domainPublisher.publish(PinProvisionedEvent(pinNumber, PinMode.Output))
//            copy(pins = pins + (pinNumber -> newPin))
//          case -\/(t) => throw new IllegalArgumentException(s"Error Provisioning output ${pinNumber}!")
//        }
//      },
//      nonEmpty = pin =>
//        (pin.pinMode == PinMode.Output).fold(
//          this,
//          throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode}!!")
//        )
//    )
//
//  def provisionGpioInputPin(pinNumber: Int, pudMode: PudMode = PudDown): GpioBoard =
//    applyOnPin[GpioBoard](pinNumber,
//      empty = () => {
//        inputPin(pinNumber, pudMode, ObservablePinConf.refreshInterval)(domainPublisher)(sendReceiveHandler) match {
//          case \/-(newPin) =>
//            domainPublisher.publish(PinProvisionedEvent(pinNumber, PinMode.Input))
//            copy(pins = pins + (pinNumber -> newPin))
//          case -\/(t) => throw t
//        }
//      },
//      nonEmpty = pin =>
//        (pin.pinMode == PinMode.Input).fold(
//          this,
//          throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode}!!")
//        )
//    )
//
//  def provisionDefaultGpioPins[A: ClassTag](pinNumbers: Int*): GpioBoard = {
//    val gpiooutputpin = implicitly[ClassTag[GpioOutputPin]]
//    val gpioinputpin = implicitly[ClassTag[GpioInputPin]]
//
//    pinNumbers.foldLeft[GpioBoard](this)((currentBoard, currentPinNumber) =>
//      implicitly[ClassTag[A]] match {
//        case `gpiooutputpin` => currentBoard.provisionGpioOutputPin(currentPinNumber)
//        case `gpioinputpin` => currentBoard.provisionGpioInputPin(currentPinNumber)
//      }
//    )
//  }
//
//  def unprovisionGpioPin(pinNumber: Int): GpioBoard =
//    applyOnPin[GpioBoard](pinNumber,
//      empty = () => this,
//      nonEmpty = pin =>
//        (if (pin.isInstanceOf[GpioInputPin]) close(pin.asInstanceOf[GpioInputPin])(domainPublisher)(sendReceiveHandler)
//        else close(pin.asInstanceOf[GpioOutputPin])(domainPublisher)(sendReceiveHandler)) match {
//          case \/-(newPin) => copy(pins = pins - pinNumber)
//          case -\/(t) => throw t
//        })
//
//  def writePinValue(pinNumber: Int, newValue: PinValue): GpioBoard =
//    applyOnPin[GpioBoard](pinNumber,
//      empty = () => throw new IllegalArgumentException(s"Pin $pinNumber is not initialised!!"),
//      nonEmpty = pin =>
//        (pin.pinMode == PinMode.Output).fold(
//          writeValue(pin.asInstanceOf[GpioOutputPin], newValue)(domainPublisher)(sendReceiveHandler) match {
//            case \/-(newPin) => copy(pins = pins + (pin.pinNumber -> newPin))
//            case -\/(t) => throw t
//          },
//          throw new IllegalArgumentException(s"${pinNumber} is already provisioned with different pinMode - ${pin.pinMode}!!")
//        )
//    )
//
//  def readPinValue(pinNumber: Int): Option[PinValue] =
//    applyOnPin[Option[PinValue]](pinNumber,
//      empty = () => None,
//      nonEmpty = pin => (if (pin.isInstanceOf[GpioInputPin]) readValue(pin.asInstanceOf[GpioInputPin]) else readValue(pin.asInstanceOf[GpioOutputPin])) (sendReceiveHandler) match {
//        case \/-(value) => Some(value)
//        case -\/(t) => throw t
//      })
//
//  def shutdown(): GpioBoard = {
//    val newBoard = pins.values.partition(_.isInstanceOf[GpioInputPin]) match {
//      case (input, output) => List(input.map(p => close(p.asInstanceOf[GpioInputPin])(domainPublisher)(sendReceiveHandler)), output.map(p => close(p.asInstanceOf[GpioOutputPin])(domainPublisher)(sendReceiveHandler))).flatten.sequenceU match {
//        case \/-(newPins: List[GpioPin]) => copy(pins = newPins.map(newPin => (newPin.pinNumber -> newPin)).toMap)
//        case -\/(t) => throw t
//      }
//    }
//    domainPublisher.publish(GpioBoardShutDownEvent())
//    newBoard
//  }
//
//  private def applyOnPin[A](pinNumber: Int, empty: () => A, nonEmpty: GpioPin => A): A =
//    pins.values.find(_.pinNumber == pinNumber) match {
//      case None => empty()
//      case Some(pin) => nonEmpty(pin)
//    }
//
//}
//
//object GpioBoard {
//
//  type PinManipulation = ValidationNel[String, GpioBoard]
//  type GpioBoardState = State[GpioBoard, Unit]
//
//  def provisionGpioOutputPin(pinNumber: Int, value: PinValue = PinValue.Low, default: PinValue = PinValue.Low) =
//    modify[GpioBoard] { gpioBoard =>
//      gpioBoard.provisionGpioOutputPin(pinNumber, value, default)
//    }
//
//  def provisionGpioPwmOutputPin(pinNumber: Int, value: PinValue = Pwm(0), default: PinValue = Pwm(0)) =
//    modify[GpioBoard] { gpioBoard =>
//      gpioBoard.provisionGpioOutputPin(pinNumber, value, default)
//    }
//
//  def provisionDefaultGpioOutputPins(pinNumbers: Int*) =
//    modify[GpioBoard] { gpioBoard =>
//      gpioBoard.provisionDefaultGpioPins[GpioOutputPin](pinNumbers: _*)
//    }
//
//  def provisionGpioInputPin(pinNumber: Int, pudMode: PudMode = PudMode.PudDown) =
//    modify[GpioBoard] { gpioBoard =>
//      gpioBoard.provisionGpioInputPin(pinNumber, pudMode)
//    }
//
//  def provisionDefaultGpioInputPins(pinNumbers: Int*) =
//    modify[GpioBoard] { gpioBoard =>
//      gpioBoard.provisionDefaultGpioPins[GpioInputPin](pinNumbers: _*)
//    }
//
//  def subscribeOneOffPinValueChangedEvent(pinNumber: Int, direction: Direction, eventOn: GpioBoardState) =
//    gets[GpioBoard, Unit] { gpioBoard =>
//      var subscription: Subscription = null
//      val handler: PartialFunction[DomainEvent, Unit] = {
//        case PinValueChangedEvent(`pinNumber`, `direction`, _) =>
//          subscription.unsubscribe()
//          eventOn.run(gpioBoard)
//        case _ =>
//      }
//      subscription = gpioBoard.domainPublisher.subscribe(handler)
//    }
//
//  def readValue(pinNumber: Int) =
//    gets[GpioBoard, Option[PinValue]] { gpioBoard =>
//      gpioBoard.readPinValue(pinNumber)
//    }
//
//  def writeValue(pinNumber: Int, newValue: PinValue) =
//    modify[GpioBoard] { gpioBoard =>
//      gpioBoard.writePinValue(pinNumber, newValue)
//    }
//
//  def shutdown() =
//    modify[GpioBoard] { gpioBoard =>
//      gpioBoard.shutdown()
//    }
//}
