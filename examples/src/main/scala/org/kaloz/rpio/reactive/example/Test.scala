package org.kaloz.rpio.reactive.example

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.GpioOutputPin
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel
import rx.lang.scala.Observer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Test extends App with StrictLogging {

  implicit val protocolHandlerFactory = PiGpioSocketChannel

  val countDownLatch = new CountDownLatch(1)

  val closed = for {
    pins <- Future.sequence(Seq(GpioOutputPin.applyAsync(pin = 25, value = High, default = Low), GpioOutputPin.applyAsync(pin = 16, value = High), GpioOutputPin.applyAsync(pin = 12, value = High)))
    subscriptions <- Future.successful(pins(0).subject.filter(_ == PinClosedEvent(25)).subscribe(Observer[Event]((event: Event) => println(event))))
    _ <- Future(Thread.sleep(2000))
    low <- pins(0).writeValue(Low)
    _ <- Future(Thread.sleep(2000))
    high <- low.writeValue(High)
    _ <- Future(Thread.sleep(2000))
    closed <- Future.sequence(pins.map(_.close()))
  } yield (closed, subscriptions)

  closed.onSuccess {
    case x =>
      println(x)
//      subs.unsubscribe()
      countDownLatch.countDown()
  }

  countDownLatch.await()
}
