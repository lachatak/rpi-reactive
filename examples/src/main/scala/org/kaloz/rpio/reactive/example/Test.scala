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
    pins <- Future.sequence(Seq(GpioOutputPin.applyAsync(pin = 25), GpioOutputPin.applyAsync(pin = 16), GpioOutputPin.applyAsync(pin = 21), GpioOutputPin.applyAsync(pin = 12, value = Pwm(0), default = Pwm(0))))
    subscriptions <- Future.successful(pins(3).subject.subscribe(Observer[Event]((event: Event) => println(event))))
    _ <- Future(Thread.sleep(2000))
    pin0_1 <- pins(0).writeValue(High)
    _ <- Future(Thread.sleep(5000))
    pin1_1 <- pins(1).writeValue(High)
    pin3_1 <- pins(3).writeValue(Pwm(100))
    _ <- Future(Thread.sleep(5000))
    pin3_2 <- pin3_1.writeValue(Pwm(255))
    _ <- Future(Thread.sleep(5000))
    closed <- Future.sequence(Seq(pin0_1, pin1_1, pin3_1).map(_.close()))
  } yield (closed, subscriptions)

  closed.onSuccess {
    case x =>
      println(x)
      //      subs.unsubscribe()
      countDownLatch.countDown()
  }



  countDownLatch.await()
}
