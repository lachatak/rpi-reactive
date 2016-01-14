package org.kaloz.rpio.reactive.example

import java.util.concurrent.CountDownLatch

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.GpioOutputPin
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Test extends App with StrictLogging {

  implicit val protocolHandlerFactory = PiGpioSocketChannel

  val countDownLatch = new CountDownLatch(1)

  val closed = for {
    pins <- Future.sequence(Seq(GpioOutputPin(pin = 25, value = High), GpioOutputPin(pin = 16, value = High), GpioOutputPin(pin = 12, value = High)))
    //    _ <- Future(Thread.sleep(5000))
    //    _ <- pins(0).writeValue(Low)
    _ <- Future(Thread.sleep(5000))
    _ <- pins(0).writeValue(High)
    _ <- Future(Thread.sleep(5000))
    closed <- Future.sequence(pins.map(_.close()))
  } yield closed

  val pin = Await.result(GpioOutputPin(pin = 25, value = High), 10 second)

  Stream.continually {
    Thread.sleep(10)
    logger.info("READ!!!")
    Await.result(pin.readValue().mapTo[ReadValueResponse], 30 second)
  }.foreach(x => logger.info(x.toString))


  closed.onComplete {
    case x =>
      pin.close()
      Thread.sleep(1000)
      countDownLatch.countDown()
  }

  countDownLatch.await()
}
