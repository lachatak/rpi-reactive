package org.kaloz.rpio.reactive.domain

import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

sealed abstract class GpioPin(pin: Int, value: PinValue, mode: PinMode, pudMode: PudMode, default: PinValue)(implicit protocolHandlerFactory: ProtocolHandlerFactory) {

  val protocolHandler = protocolHandlerFactory(Some(pin))
  Await.ready(Future.sequence(Seq(protocolHandler.request(ChangeModeRequest(pin, mode))
//    protocolHandler.request(ChangePudRequest(pin, pudMode)),
//    protocolHandler.request(WriteValueRequest(pin, value))
  )), 30 second)

  def readValue(): Future[ReadValueResponse] = protocolHandler.request(ReadValueRequest(pin)).mapTo[ReadValueResponse]

  def writeValue(value: PinValue): Future[WriteValueResponse] = protocolHandler.request(WriteValueRequest(pin, value)).mapTo[WriteValueResponse]

  def close(): Future[Unit] = writeValue(default).flatMap(x => protocolHandler.close()).mapTo[Unit]
}

object GpioOutputPin {
  def apply(pin: Int,
            value: PinValue = PinValue.Low,
            mode: PinMode = PinMode.Output,
            pudMode: PudMode = PudMode.PudOff,
            default: PinValue = PinValue.Low)
           (implicit protocolHandlerFactory: ProtocolHandlerFactory): Future[GpioOutputPin] = Future {
    new GpioOutputPin(pin,
      value,
      mode,
      pudMode,
      default)
  }
}

class GpioOutputPin(pin: Int,
                    value: PinValue,
                    mode: PinMode,
                    pudMode: PudMode,
                    default: PinValue)
                   (implicit protocolHandlerFactory: ProtocolHandlerFactory)
  extends GpioPin(pin, value, mode, pudMode, default)

object GpioInputPin {
  def apply(pin: Int,
            value: PinValue = PinValue.Low,
            mode: PinMode = PinMode.Output,
            pudMode: PudMode = PudMode.PudOff,
            default: PinValue = PinValue.Low)
           (implicit protocolHandlerFactory: ProtocolHandlerFactory): Future[GpioInputPin] = Future {
    new GpioInputPin(pin,
      value,
      mode,
      pudMode,
      default)
  }
}

class GpioInputPin(pin: Int,
                   value: PinValue = PinValue.Low,
                   mode: PinMode = PinMode.Input,
                   pudMode: PudMode = PudMode.PudUp,
                   default: PinValue = PinValue.Low)
                  (implicit protocolHandlerFactory: ProtocolHandlerFactory)
  extends GpioPin(pin, value, mode, pudMode, default)

