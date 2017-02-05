package org.kaloz.rpio.reactive.example

//import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import cats.data.{EitherT, NonEmptyList}
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
//import monix.eval.Task
//import org.kaloz.rpio.reactive.domain.Direction._
//import org.kaloz.rpio.reactive.domain.PinValue
import org.kaloz.rpio.reactive.domain.api.{OpenNotificationChannelRequest, OpenNotificationChannelResponse, ReadAllPinValuesRequest, ReadAllPinValuesResponse}
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.PiGpioService
//import org.kaloz.rpio.reactive.domain.GpioBoard
//import org.kaloz.rpio.reactive.domain.GpioBoard._
//import org.kaloz.rpio.reactive.domain.PinValue._
//import org.kaloz.rpio.reactive.domain.PudMode._
//import org.kaloz.rpio.reactive.domain.service.DomainPublisher
import monix.execution.Scheduler.Implicits.global
import monix.cats._
//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.PiGpioAkkaStreamsClient

//object Test extends App with StrictLogging {
//
//  val countDownLatch = new CountDownLatch(1)
//
//  implicit val sendReceiveHandler = PiGpioSocketChannel
//  implicit val domainPublisher = DomainPublisher
//
//  domainPublisher.subscribe {
//    case x => println(x)
//  }
//
//  def initMotor(): GpioBoardState = for {
//    _ <- provisionGpioInputPin(17, PudUp)
//    _ <- provisionDefaultGpioOutputPins(25, 16, 21)
//    _ <- provisionGpioPwmOutputPin(12)
//    value <- readValue(12)
//    _ <- subscribeOneOffPinValueChangedEvent(17, Falling_Edge, startMotor)
//  } yield {
//    println(value)
//  }
//
//  def startMotor(): GpioBoardState = for {
//    _ <- writeValue(25, High)
//    _ <- writeValue(16, High)
//    _ <- writeValue(12, Pwm(100))
//    _ <- subscribeOneOffPinValueChangedEvent(17, Falling_Edge, stopMotor)
//  } yield {}
//
//  def stopMotor(): GpioBoardState = for {
//    value <- readValue(17)
//    _ <- shutdown()
//  } yield {
//    println(value)
//    countDownLatch.countDown()
//  }
//
//  initMotor run (new GpioBoard())
//
//  countDownLatch.await()
//}

object Akka extends App with StrictLogging {

  private implicit val system = ActorSystem("pigpio-client")
  private implicit val mater = ActorMaterializer()
//  private implicit val executionContext = system.dispatcher

  val client = PiGpioAkkaStreamsClient()
  val server = PiGpioService(client)

  Source.fromPublisher(client.gpioNotificationResponsePublisher).runForeach(println)

  println("start")
  val read = for {
    notify <- TaskService[OpenNotificationChannelResponse](server.sendReceive(OpenNotificationChannelRequest()))
    br1 <- TaskService[ReadAllPinValuesResponse](server.sendReceive(ReadAllPinValuesRequest()))
  //    _ <- EitherT(client.sendReceive(GpioRequest(NB, Some(notify.p3), Some(1 << 25))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(MODES, Some(17), Some(1))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(PUD, Some(17), Some(2))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(MODES, Some(25), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(WRITE, Some(25), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(MODES, Some(16), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(WRITE, Some(16), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(MODES, Some(21), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(WRITE, Some(21), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(MODES, Some(12), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(PWM, Some(12), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(WRITE, Some(25), Some(1))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(WRITE, Some(16), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(PWM, Some(12), Some(100))))
  } yield (notify, br1)

  read.value.runAsync.foreach(println)
  //  for {
  //    r <- read
  //    (notify, br1) = r
  //    value <- EitherT(client.sendReceive(GpioRequest(HWVER)))
  //    _ <- EitherT(client.sendReceive(GpioRequest(WRITE, Some(25), Some(0))))
  //    _ <- EitherT(client.sendReceive(GpioRequest(NC, Some(notify))))
  //  } yield println(s"$value - $br1")
  //
  //
  //  client.sendReceive(GpioRequest(MODES, Some(25), Some(0))).onComplete(r => println(s"1 - $r"))
  //  client.sendReceive(GpioRequest(WRITE, Some(25), Some(1))).onComplete(r => println(s"2 - $r"))
  //  client.sendReceive(GpioRequest(MODES, Some(16), Some(0))).onComplete(r => println(s"3 - $r"))

  //  Thread.sleep(10)
  //
  //  controlQueue.offer(Request(WRITE, 25, 0)) //value off
  //
  //  Thread.sleep(10)
  //
  //  controlQueue.offer(Request(WRITE, 25, 1)) //value off
  //  controlQueue.offer(Request(WRITE, 16, 0)) //value off
  //  controlQueue.offer(Request(PWM, 12, 0)) //pwm 0
  //
  //  Thread.sleep(10)
  //
  //  controlQueue.offer(Request(WRITE, 25, 0)) //value off
  //  controlQueue.offer(Request(WDOG, 25, 0)) //value off
  //
  //  Thread.sleep(1000)
  //  notificationQueue.offer(Request(NC, 0, 0)) //swith off notification listener


}


//import scala.language.{higherKinds, implicitConversions}

object TaskService {

  type TaskService[A] = EitherT[Task, NonEmptyList[String], A]

  def apply[A](f: Task[NonEmptyList[String] Either A]): TaskService[A] = {
    EitherT[Task, NonEmptyList[String], A](f)
  }

}




