package org.kaloz.rpio.reactive

import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._

package object domain {

  object DomainApi {

    trait Request

    trait Response

    trait Event

    trait ProtocolHandler {
      def request(request: Request): Response

      def close(): Unit
    }

    trait ProtocolHandlerFactory {
      def apply(pinNumber: Option[Int] = None): ProtocolHandler
    }

    case class ChangePinModeRequest(pinNumber: Int, pinMode: PinMode) extends Request

    case class ChangePinModeResponse(result: Int) extends Response

    case class ChangePudModeRequest(pinNumber: Int, pudMode: PudMode) extends Request

    case class ChangePudModeResponse(result: Int) extends Response

    case class ReadValueRequest(pinNumber: Int) extends Request

    case class ReadValueResponse(value: PinValue) extends Response

    case class WriteValueRequest(pinNumber: Int, value: PinValue) extends Request

    case class WriteValueResponse(result: Int) extends Response

    case class VersionRequest() extends Request

    case class VersionResponse(version: Int) extends Response

    case class PinProvisionedEvent(pinNumber: Int, pinMode: PinMode) extends Event

    case class PinValueChangedEvent(pinNumber: Int, direction: Direction, value: PinValue) extends Event

    case class PinClosedEvent(pinNumber: Int) extends Event

    case class GpioBoardShutDownEvent() extends Event

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
