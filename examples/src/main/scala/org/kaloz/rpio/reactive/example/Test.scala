package org.kaloz.rpio.reactive.example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import cats.data.EitherT
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import org.kaloz.rpio.reactive.domain.GpioPin.PinNumber
import org.kaloz.rpio.reactive.domain.PinValue.Pwm
import org.kaloz.rpio.reactive.domain.api.{ChangePinModeRequest, ChangePudModeRequest, CloseNotificationChannelRequest, SubscribeNotificationRequest, VersionRequest, WriteValueRequest}
import org.kaloz.rpio.reactive.domain.{PinMode, PinValue, PudMode}
import org.kaloz.rpio.reactive.domain.api.{OpenNotificationChannelRequest, ReadAllPinValuesRequest}
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.{GpioAssembler, PiGpioAkkaStreamsClient, PiGpioAkkaStreamsSendReceiver}
import monix.execution.Scheduler.Implicits.global
import monix.cats.monixToCatsMonad

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

  val client = PiGpioAkkaStreamsClient()
  val service = PiGpioAkkaStreamsSendReceiver(client)

  Source.fromPublisher(client.gpioNotificationResponsePublisher).runForeach(println)

  import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.GpioAssembler._

  println("start")
  val read = for {
    notify <- TaskService(service.sendReceive(OpenNotificationChannelRequest()))
    br1 <- TaskService(service.sendReceive(ReadAllPinValuesRequest()))
      _ <- TaskService(service.sendReceive(SubscribeNotificationRequest(PinNumber(25), notify.handler)))
      _ <- TaskService(service.sendReceive(ChangePinModeRequest(PinNumber(17), PinMode.Input)))
      _ <- TaskService(service.sendReceive(ChangePudModeRequest(PinNumber(17), PudMode.PudUp)))
      _ <- TaskService(service.sendReceive(ChangePinModeRequest(PinNumber(25), PinMode.Output)))
      _ <- TaskService(service.sendReceive(WriteValueRequest(PinNumber(25), PinValue.Low)))
      _ <- TaskService(service.sendReceive(ChangePinModeRequest(PinNumber(16), PinMode.Output)))
      _ <- TaskService(service.sendReceive(WriteValueRequest(PinNumber(16), PinValue.Low)))
      _ <- TaskService(service.sendReceive(ChangePinModeRequest(PinNumber(21), PinMode.Output)))
      _ <- TaskService(service.sendReceive(WriteValueRequest(PinNumber(21), PinValue.Low)))
      _ <- TaskService(service.sendReceive(ChangePinModeRequest(PinNumber(12), PinMode.Output)))
      _ <- TaskService(service.sendReceive(WriteValueRequest(PinNumber(12), Pwm(0))))
      _ <- TaskService(service.sendReceive(WriteValueRequest(PinNumber(25), PinValue.High)))
      _ <- TaskService(service.sendReceive(WriteValueRequest(PinNumber(16), PinValue.Low)))
      _ <- TaskService(service.sendReceive(WriteValueRequest(PinNumber(12), Pwm(100))))
  } yield (notify, br1)

    val read2 = for {
      r <- read
      (notify, br1) = r
      value <- TaskService(service.sendReceive(VersionRequest()))
      _ <- TaskService(service.sendReceive(WriteValueRequest(PinNumber(25), PinValue.Low)))
      _ <- TaskService(service.sendReceive(CloseNotificationChannelRequest(notify.handler)))
    } yield s"$value - $br1"

  read2.value.runAsync.foreach(println)


  service.sendReceive(ChangePinModeRequest(PinNumber(25), PinMode.Output)).runAsync.onComplete(r => println(s"1 - $r"))
  service.sendReceive(WriteValueRequest(PinNumber(25), PinValue.High)).runAsync.onComplete(r => println(s"2 - $r"))
  service.sendReceive(ChangePinModeRequest(PinNumber(16), PinMode.Output)).runAsync.onComplete(r => println(s"3 - $r"))

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

object TaskService {

  type TaskService[A] = EitherT[Task, String, A]

  def apply[A](f: Task[String Either A]): TaskService[A] = {
    EitherT[Task, String, A](f)
  }


}






