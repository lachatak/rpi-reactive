package org.kaloz.rpio.reactive.example

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.Direction._
import org.kaloz.rpio.reactive.domain.GpioOutputPin
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.async.ObservablePin
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Test extends App with StrictLogging {

  implicit val protocolHandlerFactory = PiGpioSocketChannel

  val countDownLatch = new CountDownLatch(1)

  val closed = for {
    pins <- Future.sequence(Seq(GpioOutputPin(pin = 25, value = High, default = Low), GpioOutputPin(pin = 16, value = High), GpioOutputPin(pin = 12, value = High)))
    _ <- Future(Thread.sleep(2000))
    _ <- pins(0).writeValue(Low)
    _ <- Future(Thread.sleep(2000))
    _ <- pins(0).writeValue(High)
    _ <- Future(Thread.sleep(2000))
    closed <- Future.sequence(pins.map(_.close()))
  } yield closed

  val subscription = ObservablePin(25, Rising_Edge).subscribe(value => println(value))

  closed.onComplete {
    case x =>
      subscription.unsubscribe()
      countDownLatch.countDown()
  }

  countDownLatch.await()
}
