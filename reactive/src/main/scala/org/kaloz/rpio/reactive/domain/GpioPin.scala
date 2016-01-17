package org.kaloz.rpio.reactive.domain

import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz._
import Scalaz._

object GpioPin {
  def apply(pin: Int)(implicit protocolHandlerFactory: ProtocolHandlerFactory): Future[GpioPin] =
    Future(new GpioPin(pin))
}

class GpioPin(pin: Int)(implicit protocolHandlerFactory: ProtocolHandlerFactory) {

  val protocolHandler = protocolHandlerFactory(pin.some)

  def readValue(): Future[ReadValueResponse] =
    protocolHandler.request(ReadValueRequest(pin)).mapTo[ReadValueResponse]

  def changePinMode(pinMode: PinMode): Future[ChangePinModeResponse] =
    protocolHandler.request(ChangePinModeRequest(pin, pinMode)).mapTo[ChangePinModeResponse]

  def changePudMode(pudMode: PudMode): Future[ChangePudModeResponse] =
    protocolHandler.request(ChangePudModeRequest(pin, pudMode)).mapTo[ChangePudModeResponse]

  def writeValue(value: PinValue): Future[WriteValueResponse] =
    protocolHandler.request(WriteValueRequest(pin, value)).mapTo[WriteValueResponse]

  def close(): Future[Unit] = protocolHandler.close()
}

object GpioOutputPin {
  def apply(pin: Int,
            value: PinValue = PinValue.Low,
            default: PinValue = PinValue.Low)
           (implicit protocolHandlerFactory: ProtocolHandlerFactory): Future[GpioOutputPin] = Future {
    new GpioOutputPin(pin, value, default)
  }
}

class GpioOutputPin(pin: Int,
                    value: PinValue,
                    default: PinValue)
                   (implicit protocolHandlerFactory: ProtocolHandlerFactory) extends GpioPin(pin) {

  Await.ready(
    Future.sequence(
      Seq(
        changePinMode(PinMode.Output),
        writeValue(value)
      )
    ),
    30 second)

  override def close(): Future[Unit] = writeValue(default).flatMap(_ => super.close())
}

object GpioInputPin {
  def apply(pin: Int,
            pudMode: PudMode = PudMode.PudDown)
           (implicit protocolHandlerFactory: ProtocolHandlerFactory): Future[GpioInputPin] = Future {
    new GpioInputPin(pin, pudMode)
  }
}

class GpioInputPin(pin: Int,
                   pudMode: PudMode)
                  (implicit protocolHandlerFactory: ProtocolHandlerFactory) extends GpioPin(pin) {

  Await.ready(
    Future.sequence(
      Seq(
        changePinMode(PinMode.Input),
        changePudMode(pudMode)
      )
    ),
    30 second)
}

