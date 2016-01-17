package org.kaloz.rpio.reactive.domain

import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import rx.lang.scala.subjects._
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.Scalaz._

sealed abstract class GpioPin(val pin: Int, mode: PinMode, closed: Boolean)(implicit protocolHandler: ProtocolHandler, val subject: Subject[Event]) {

  protected def changePinMode(pinMode: PinMode): Future[ChangePinModeResponse] =
    protocolHandler.request(ChangePinModeRequest(pin, pinMode)).mapTo[ChangePinModeResponse]

  def pinMode(): Future[PinMode] = Future.successful(mode)

  def readValue(): Future[PinValue]

  protected def close(): Future[Any] = protocolHandler.close()

}

object GpioOutputPin {
  def applyAsync(pin: Int,
                 value: PinValue = PinValue.Low,
                 default: PinValue = PinValue.Low,
                 closed: Boolean = false)
                (implicit protocolHandlerFactory: ProtocolHandlerFactory): Future[GpioOutputPin] = Future {

    implicit val subject = PublishSubject[Event]()
    implicit val protocolHandler = protocolHandlerFactory(pin.some)

    val newPin = new GpioOutputPin(pin, value, default, closed)

    Await.ready(
      Future.sequence(
        Seq(
          newPin.changePinMode(PinMode.Output),
          newPin.writeValue(value)
        )
      ),
      30 second)

    newPin
  }
}

case class GpioOutputPin(override val pin: Int,
                         value: PinValue,
                         default: PinValue,
                         closed: Boolean)
                        (implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) extends GpioPin(pin, PinMode.Output, closed) {


  def readValue(): Future[PinValue] = Future.successful(value)

  def writeValue(newValue: PinValue): Future[GpioOutputPin] = protocolHandler.request(WriteValueRequest(pin, newValue))
    .mapTo[WriteValueResponse].map {
    response =>
      import PinValue.pinValueToInt
      (value, newValue) match {
        case (o, n) if (o == n) => this
        case (o, n) if (o < n) =>
          subject.onNext(PinValueChangedEvent(pin, Rising_Edge, newValue))
          this.copy(value = newValue)
        case (o, n) if (o > n) =>
          subject.onNext(PinValueChangedEvent(pin, Falling_Edge, newValue))
          this.copy(value = newValue)
      }
  }

  override def close(): Future[GpioOutputPin] = for {
    _ <- writeValue(default)
    _ <- super.close()
  } yield {
    subject.onNext(PinClosedEvent(pin))
    this.copy(value = default, closed = true)
  }

}

object GpioInputPin {
  def applyAsync(pin: Int,
                 pudMode: PudMode = PudMode.PudDown,
                 closed: Boolean = false)
                (implicit protocolHandlerFactory: ProtocolHandlerFactory): Future[GpioInputPin] = Future {

    implicit val protocolHandler = protocolHandlerFactory(pin.some)
    implicit val subject = PublishSubject[Event]()

    val newPin = new GpioInputPin(pin, pudMode, closed)

    Await.ready(
      Future.sequence(
        Seq(
          newPin.changePinMode(PinMode.Input),
          newPin.changePudMode(pudMode)
        )
      ).map(_ => newPin.changeListener()),
      30 second)

    newPin
  }
}

case class GpioInputPin(override val pin: Int,
                        pud: PudMode = PudDown,
                        closed: Boolean = false)
                       (implicit protocolHandler: ProtocolHandler, subject: Subject[Event]) extends GpioPin(pin, PinMode.Input, closed) with Configuration {


  private def changePudMode(pudMode: PudMode): Future[ChangePudModeResponse] =
    protocolHandler.request(ChangePudModeRequest(pin, pudMode)).mapTo[ChangePudModeResponse]

  def pudMode() = Future.successful(pud)

  def readValue(): Future[PinValue] = protocolHandler.request(ReadValueRequest(pin)).mapTo[ReadValueResponse].map(_.value)

  override def close(): Future[GpioInputPin] = super.close().map { _ =>
    subject.onNext(PinClosedEvent(pin))
    this.copy(closed = true)
  }

  def changeListener(): Future[Unit] =
    readValue().map {
      case pinValue =>
        var lastValue = pinValue
        val intervals = Observable.interval(observablePin.refreshInterval millisecond)
        intervals.subscribe(next => {
          val currentValue = Await.result(readValue(), 30 seconds)
          import PinValue.pinValueToInt
          (lastValue, currentValue) match {
            case (o, n) if (o == n) =>
            case (o, n) if (o < n) =>
              subject.onNext(PinValueChangedEvent(pin, Rising_Edge, currentValue))
            case (o, n) if (o > n) =>
              subject.onNext(PinValueChangedEvent(pin, Falling_Edge, currentValue))
          }
          lastValue = currentValue
        })
    }
}
