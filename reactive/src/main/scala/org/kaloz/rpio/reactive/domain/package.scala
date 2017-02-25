package org.kaloz.rpio.reactive

import cats.data.Kleisli
import monix.eval.Task
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.GpioPin.PinNumber
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import rx.lang.scala.Subscription

package object domain {

  object api {

    type Valid[A] = Task[Either[String, A]]
    type PinOperation[A] = Kleisli[Valid, SendReceiver, A]

    trait DomainRequest

    trait DomainResponse

    trait DomainEvent

    trait Mapper[A] {
      type R
    }

    object Mapper {
      type Aux[AA, RR] = Mapper[AA] {type R = RR}

      implicit val changePinModeRequestMapper = new Mapper[ChangePinModeRequest] {

        type R = Unit
      }

      implicit val changePudModeRequestMapper = new Mapper[ChangePudModeRequest] {
        type R = Unit
      }

      implicit val readValueRequestMapper = new Mapper[ReadValueRequest] {
        type R = ReadValueResponse
      }

      implicit val writeValueRequestMapper = new Mapper[WriteValueRequest] {
        type R = Unit
      }

      implicit val readAllPinValuesRequestMapper = new Mapper[ReadAllPinValuesRequest] {
        type R = ReadAllPinValuesResponse
      }

      implicit val subscribeNotificationRequestMapper = new Mapper[SubscribeNotificationRequest] {
        type R = Unit
      }

      implicit val closeNotificationChannelRequestMapper = new Mapper[CloseNotificationChannelRequest] {
        type R = Unit
      }

      implicit val openNotificationChannelRequestMapper = new Mapper[OpenNotificationChannelRequest] {
        type R = OpenNotificationChannelResponse
      }

      implicit val versionRequestMapper = new Mapper[VersionRequest] {
        type R = VersionResponse
      }
    }

    trait SendReceiver {

      def sendReceive[A <: DomainRequest, R](request: A)(implicit mapper: Mapper.Aux[A, R]): Valid[R]
    }

    trait PinManipulationService {

      def changePinMode(pinNumber: PinNumber, pinMode: PinMode): PinOperation[Unit]

      def changePudMode(request: ChangePudModeRequest): PinOperation[Unit]

      def readValue(request: ReadValueRequest): PinOperation[ReadValueResponse]

      def readAllValueValues(request: ReadAllPinValuesRequest): PinOperation[ReadAllPinValuesResponse]

      def writeValue(request: WriteValueRequest): PinOperation[Unit]

      def version(request: VersionRequest): PinOperation[VersionResponse]

      def openNotificationChannel(request: OpenNotificationChannelRequest): PinOperation[OpenNotificationChannelResponse]

      def closeNotificationChannel(request: CloseNotificationChannelRequest): PinOperation[Unit]

      def subscribeNotification(request: SubscribeNotificationRequest): PinOperation[Unit]

    }

    case class ChangePinModeRequest(pinNumber: PinNumber, pinMode: PinMode) extends DomainRequest

    case class ChangePudModeRequest(pinNumber: PinNumber, pudMode: PudMode) extends DomainRequest

    case class ReadValueRequest(pinNumber: PinNumber) extends DomainRequest

    case class ReadValueResponse(value: PinValue) extends DomainResponse

    case class ReadAllPinValuesRequest() extends DomainRequest

    case class ReadAllPinValuesResponse(pinValues: Int) extends DomainResponse

    case class OpenNotificationChannelRequest() extends DomainRequest

    case class OpenNotificationChannelResponse(handler: ChannelHandler) extends DomainResponse

    case class SubscribeNotificationRequest(pin: PinNumber, handler: ChannelHandler) extends DomainRequest

    case class CloseNotificationChannelRequest(handler: ChannelHandler) extends DomainRequest

    case class WriteValueRequest(pinNumber: PinNumber, value: PinValue) extends DomainRequest

    case class VersionRequest() extends DomainRequest

    case class VersionResponse(version: Int) extends DomainResponse

    case class PinProvisionedEvent(pinNumber: PinNumber, pinMode: PinMode) extends DomainEvent

    case class PinValueChangedEvent(pinNumber: PinNumber, direction: Direction, value: PinValue) extends DomainEvent

    case class PinClosedEvent(pinNumber: PinNumber) extends DomainEvent

    case class GpioBoardShutDownEvent() extends DomainEvent

  }

  object GpioPin {

    case class PinNumber(id: Int)

    sealed abstract class GpioPin(val pinNumber: PinNumber, val pinMode: PinMode, val closed: Boolean)

    case class GpioOutputPin private(override val pinNumber: PinNumber,
                                     value: PinValue,
                                     defaultValue: PinValue,
                                     override val closed: Boolean) extends GpioPin(pinNumber, PinMode.Output, closed) {

      def this(pinNumber: PinNumber, value: PinValue, defaultValue: PinValue) {
        this(pinNumber, value, defaultValue, false)
      }
    }

    object GpioOutputPin {
      def apply(pinNumber: PinNumber, value: PinValue = PinValue.Low, defaultValue: PinValue = PinValue.Low) = new GpioOutputPin(pinNumber, value, defaultValue)
    }

    case class GpioInputPin private(override val pinNumber: PinNumber,
                                    pudMode: PudMode,
                                    override val closed: Boolean,
                                    subscription: Option[Subscription]) extends GpioPin(pinNumber, PinMode.Output, closed) {

      def this(pinNumber: PinNumber, pudMode: PudMode) {
        this(pinNumber, pudMode, false, None)
      }
    }

    object GpioInputPin {
      def apply(pinNumber: PinNumber, pudMode: PudMode = PudMode.PudDown) = new GpioInputPin(pinNumber, pudMode)
    }

  }

  case class ChannelHandler(id: Int)

  object PinMode {

    sealed trait PinMode

    case object Input extends PinMode

    case object Output extends PinMode

  }

  object PinValue {

    sealed trait PinValue

    case object Low extends PinValue

    case object High extends PinValue

    case class Pwm(value: Int) extends PinValue

    implicit def intToPinValue(pinValue: Int): PinValue = pinValue match {
      case 0 => Low
      case 1 => High
      case x => Pwm(x)
    }

    implicit def pinValueToInt(pinValue: PinValue): Int = pinValue match {
      case Low => 0
      case High => 1
      case Pwm(x) => x
    }
  }

  object PudMode {

    sealed trait PudMode

    case object PudOff extends PudMode

    case object PudUp extends PudMode

    case object PudDown extends PudMode

  }

  object Direction {

    sealed trait Direction

    case object Rising_Edge extends Direction

    case object Falling_Edge extends Direction

    case object Both extends Direction


  }

}