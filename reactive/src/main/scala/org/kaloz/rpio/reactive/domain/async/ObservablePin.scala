package org.kaloz.rpio.reactive.domain.async

import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.DomainApi.{ProtocolHandlerFactory, _}
import org.kaloz.rpio.reactive.domain.{GpioInputPin, GpioPin}
import org.kaloz.rpio.reactive.domain.PinValue.{pinValueToInt, _}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object ObservablePin extends Configuration {

  def apply[A <: GpioPin](gpioPin: A, direction: Direction = Both)(implicit protocolHandlerFactory: ProtocolHandlerFactory): Observable[PinValueChangedEvent] =
    Observable[PinValueChangedEvent] { observer =>
      //      val gpioInputPin = new GpioInputPin(pin)
      var lastValue = Await.result(gpioPin.readValue(), 30 seconds)
      val intervals = Observable.interval(observablePin.refreshInterval millisecond)
      intervals.subscribe(next => {
        val currentValue = Await.result(gpioPin.readValue(), 30 seconds)

        println(currentValue)
        if (currentValue != lastValue) {
          direction match {
            case Falling_Edge | Both if (currentValue < lastValue) => observer.onNext(PinValueChangedEvent(gpioPin.pin, Falling_Edge, Low))
            case Rising_Edge | Both if (currentValue > lastValue) => observer.onNext(PinValueChangedEvent(gpioPin.pin, Rising_Edge, High))
            case _ =>
          }
          lastValue = currentValue
        }
      })

      Subscription()
      //      Subscription {
      //        gpioPin.close()
      //      }
    }
}
