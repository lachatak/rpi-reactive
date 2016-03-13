package org.kaloz.rpio.reactive.domain.service

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.GpioPin._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import org.kaloz.rpio.reactive.domain._
import org.kaloz.rpio.reactive.domain.api._
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration.FiniteDuration
import scalaz.Kleisli._
import scalaz.Scalaz._
import scalaz._

trait PinManipulationServiceImpl extends PinManipulationService with StrictLogging {

  def changePinMode(request: ChangePinModeRequest): PinOperation[ChangePinModeResponse] = kleisli[Valid, SendReceiveHandler, ChangePinModeResponse] { (protocolHandler: SendReceiveHandler) =>
    protocolHandler.sendReceive[ChangePinModeResponse](request)
  }

  def changePudMode(request: ChangePudModeRequest): PinOperation[ChangePudModeResponse] = kleisli[Valid, SendReceiveHandler, ChangePudModeResponse] { (protocolHandler: SendReceiveHandler) =>
    protocolHandler.sendReceive[ChangePudModeResponse](request)
  }

  def readValue(request: ReadValueRequest): PinOperation[ReadValueResponse] = kleisli[Valid, SendReceiveHandler, ReadValueResponse] { (protocolHandler: SendReceiveHandler) =>
    protocolHandler.sendReceive[ReadValueResponse](request)
  }

  def writeValue(request: WriteValueRequest): PinOperation[WriteValueResponse] = kleisli[Valid, SendReceiveHandler, WriteValueResponse] { (protocolHandler: SendReceiveHandler) =>
    protocolHandler.sendReceive[WriteValueResponse](request)
  }

  def version(request: VersionRequest): PinOperation[VersionResponse] = kleisli[Valid, SendReceiveHandler, VersionResponse] { (protocolHandler: SendReceiveHandler) =>
    protocolHandler.sendReceive[VersionResponse](request)
  }
}

trait OutputPinManipulationServiceImpl extends PinManipulationServiceImpl {

  def outputPin(pinNumber: Int, value: PinValue = PinValue.Low, defaultValue: PinValue = PinValue.Low): PinOperation[GpioOutputPin] =
    for {
      _ <- changePinMode(ChangePinModeRequest(pinNumber, PinMode.Output))
      _ <- writeValue(WriteValueRequest(pinNumber, value))
    } yield GpioOutputPin(pinNumber, value, defaultValue)


  def readValue(outputPin: GpioOutputPin): PinOperation[PinValue] = kleisli[Valid, SendReceiveHandler, PinValue] { _ =>
    \/-(outputPin.value)
  }

  def writeValue(outputPin: GpioOutputPin, newValue: PinValue)(implicit domainPublisher: DomainPublisher): PinOperation[GpioOutputPin] =
    writeValue(WriteValueRequest(outputPin.pinNumber, newValue)).map { _ =>
      (outputPin.value, newValue) match {
        case (o, n) if (o == n) => outputPin
        case (o, n) =>
          domainPublisher.publish(PinValueChangedEvent(outputPin.pinNumber, (o < n).fold(Rising_Edge, Falling_Edge), newValue))
          outputPin.copy(value = newValue)
      }
    }

  def close(outputPin: GpioOutputPin)(implicit domainPublisher: DomainPublisher): PinOperation[GpioOutputPin] =
    writeValue(outputPin, outputPin.defaultValue).map { _ =>
      domainPublisher.publish(PinClosedEvent(outputPin.pinNumber))
      outputPin.copy(closed = true, value = outputPin.defaultValue)
    }

}

trait InputPinManipulationServiceImpl extends PinManipulationServiceImpl {

  def inputPin(pinNumber: Int, pudMode: PudMode = PudMode.PudDown, refreshInterval: FiniteDuration)(implicit domainPublisher: DomainPublisher): PinOperation[GpioInputPin] = {
    val newInputPin = for {
      _ <- changePinMode(ChangePinModeRequest(pinNumber, PinMode.Input))
      _ <- changePudMode(ChangePudModeRequest(pinNumber, pudMode))
    } yield GpioInputPin(pinNumber, pudMode)
    newInputPin.flatMap(pin => inputPinListener(pin, refreshInterval))
  }

  def readValue(inputPin: GpioInputPin): PinOperation[PinValue] = readValue(ReadValueRequest(inputPin.pinNumber)).map(_.value)

  private def inputPinListener(inputPin: GpioInputPin, refreshInterval: FiniteDuration)(implicit domainPublisher: DomainPublisher): PinOperation[GpioInputPin] = {

    def subscription(firstValue: PinValue, inputPin: GpioInputPin): PinOperation[Subscription] = kleisli[Valid, SendReceiveHandler, Subscription] { (protocolHandler: SendReceiveHandler) =>
      \/.fromTryCatchThrowable[Subscription, Throwable] {
        inputPin.subscription.foreach(_.unsubscribe())
        var lastValue = firstValue
        Observable.interval(refreshInterval).subscribe(
          next => {
            protocolHandler.sendReceive[ReadValueResponse](ReadValueRequest(inputPin.pinNumber)).map { currentValue =>
              (lastValue, currentValue.value) match {
                case (o, n) if (o == n) =>
                case (o, n) =>
                  domainPublisher.publish(PinValueChangedEvent(inputPin.pinNumber, (o < n).fold(Rising_Edge, Falling_Edge), n))
                  lastValue = n
              }
            }
          })
      }
    }

    for {
      firstValue <- readValue(inputPin)
      s <- subscription(firstValue, inputPin)
    } yield inputPin.copy(subscription = s.some)
  }

  def close(inputPin: GpioInputPin)(implicit domainPublisher: DomainPublisher): PinOperation[GpioInputPin] = kleisli[Valid, SendReceiveHandler, GpioInputPin] { _ =>
    \/.fromTryCatchThrowable[GpioInputPin, Throwable] {
      inputPin.subscription.foreach(_.unsubscribe())
      domainPublisher.publish(PinClosedEvent(inputPin.pinNumber))
      inputPin.copy(closed = true, subscription = None)
    }
  }
}

object PinManipulationService extends InputPinManipulationServiceImpl with OutputPinManipulationServiceImpl

