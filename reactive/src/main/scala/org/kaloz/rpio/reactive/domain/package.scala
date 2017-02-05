package org.kaloz.rpio.reactive

import cats.data.{Kleisli, NonEmptyList}
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import rx.lang.scala.Subscription
import monix.eval.Task

package object domain {

  object api {

    type Valid[B] = Task[Either[NonEmptyList[String], B]]
    type PinOperation[B] = Kleisli[Valid, SendReceiveHandler, B]

    trait DomainRequest

    trait DomainResponse

    trait DomainEvent

    trait SendReceiveHandler {
      def sendReceive[A <: DomainRequest, B <: DomainResponse](request: A): Valid[B]
    }

    trait PinManipulationService {

      def changePinMode(request: ChangePinModeRequest): PinOperation[SuccessFulEmptyResponse]

      def changePudMode(request: ChangePudModeRequest): PinOperation[SuccessFulEmptyResponse]

      def readValue(request: ReadValueRequest): PinOperation[ReadValueResponse]

      def writeValue(request: WriteValueRequest): PinOperation[SuccessFulEmptyResponse]

      def version(request: VersionRequest): PinOperation[VersionResponse]

    }

    case class ChangePinModeRequest(pinNumber: Int, pinMode: PinMode) extends DomainRequest

    case class ChangePudModeRequest(pinNumber: Int, pudMode: PudMode) extends DomainRequest

    case class ReadValueRequest(pinNumber: Int) extends DomainRequest

    case class ReadValueResponse(value: PinValue) extends DomainResponse

    case class ReadAllPinValuesRequest() extends DomainRequest

    case class ReadAllPinValuesResponse(pinValues:Int) extends DomainResponse

    case class OpenNotificationChannelRequest() extends DomainRequest

    case class OpenNotificationChannelResponse(handler: Int) extends DomainResponse

    case class SubscribeNotificationRequest(pin: Int, handler: Int) extends DomainRequest

    case class CloseNotificationChannelRequest() extends DomainRequest

    case class WriteValueRequest(pinNumber: Int, value: PinValue) extends DomainRequest

    case class VersionRequest() extends DomainRequest

    case class VersionResponse(version: Int) extends DomainResponse

    case class SuccessFulEmptyResponse() extends DomainResponse

    case class PinProvisionedEvent(pinNumber: Int, pinMode: PinMode) extends DomainEvent

    case class PinValueChangedEvent(pinNumber: Int, direction: Direction, value: PinValue) extends DomainEvent

    case class PinClosedEvent(pinNumber: Int) extends DomainEvent

    case class GpioBoardShutDownEvent() extends DomainEvent

  }

  object GpioPin {

    sealed abstract class GpioPin(val pinNumber: Int, val pinMode: PinMode, val closed: Boolean)

    case class GpioOutputPin private(override val pinNumber: Int,
                                     value: PinValue,
                                     defaultValue: PinValue,
                                     override val closed: Boolean) extends GpioPin(pinNumber, PinMode.Output, closed) {

      def this(pinNumber: Int, value: PinValue, defaultValue: PinValue) {
        this(pinNumber, value, defaultValue, false)
      }
    }

    object GpioOutputPin {
      def apply(pinNumber: Int, value: PinValue = PinValue.Low, defaultValue: PinValue = PinValue.Low) = new GpioOutputPin(pinNumber, value, defaultValue)
    }

    case class GpioInputPin private(override val pinNumber: Int,
                                    pudMode: PudMode,
                                    override val closed: Boolean,
                                    subscription: Option[Subscription]) extends GpioPin(pinNumber, PinMode.Output, closed) {

      def this(pinNumber: Int, pudMode: PudMode) {
        this(pinNumber, pudMode, false, None)
      }
    }

    object GpioInputPin {
      def apply(pinNumber: Int, pudMode: PudMode = PudMode.PudDown) = new GpioInputPin(pinNumber, pudMode)
    }

  }

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