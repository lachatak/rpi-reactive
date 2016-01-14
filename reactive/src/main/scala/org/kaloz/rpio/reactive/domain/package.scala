package org.kaloz.rpio.reactive

import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._

import scala.concurrent.Future

package object domain {

  object DomainApi {

    trait Request

    trait Response

    trait ProtocolHandler {
      def request(request: Request): Future[Response]

      def close(): Future[Unit]
    }

    trait ProtocolHandlerFactory {
      def apply(pin: Option[Int] = None): ProtocolHandler
    }

    case class ChangeModeRequest(pin: Int, pinMode: PinMode) extends Request

    case class ChangeModeResponse(result: Int) extends Response

    case class ChangePudRequest(pin: Int, pudMode: PudMode) extends Request

    case class ChangePudResponse(result: Int) extends Response

    case class ReadValueRequest(pin: Int) extends Request

    case class ReadValueResponse(value: PinValue) extends Response

    case class WriteValueRequest(pin: Int, value: PinValue) extends Request

    case class WriteValueResponse(result: Int) extends Response

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

  }

  object PudMode {

    sealed trait PudMode

    case object PudOff extends PudMode

    case object PudUp extends PudMode

    case object PudDown extends PudMode

  }

}
