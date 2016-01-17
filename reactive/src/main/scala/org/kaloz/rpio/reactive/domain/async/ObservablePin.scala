package org.kaloz.rpio.reactive.domain.async

import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.DomainApi.{ProtocolHandlerFactory, _}
import org.kaloz.rpio.reactive.domain.GpioPin
import org.kaloz.rpio.reactive.domain.PinValue.{pinValueToInt, _}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ObservablePin extends Configuration {

  def apply(pin: Int, direction: Direction = Both)(implicit protocolHandlerFactory: ProtocolHandlerFactory): Observable[PinChangedEvent] =
    Observable[PinChangedEvent] { observer =>
      val gpioPin = new GpioPin(pin)
      var lastValue = Await.result(gpioPin.readValue(), 30 seconds).value
      val intervals = Observable.interval(observablePin.refreshInterval millisecond).zip(Observable.from(gpioPin.readValue()).repeat).collect { case (_, response) => response }
      intervals.subscribe(next => next match {
        case ReadValueResponse(currentValue) if (currentValue != lastValue) =>
          direction match {
            case Falling_Edge | Both if (currentValue < lastValue) => observer.onNext(PinChangedEvent(pin, Falling_Edge, Low))
            case Rising_Edge | Both if (currentValue > lastValue) => observer.onNext(PinChangedEvent(pin, Rising_Edge, High))
            case _ =>
          }
          lastValue = currentValue
        case _ =>
      })

      Subscription {
        gpioPin.close()
      }
    }
}
