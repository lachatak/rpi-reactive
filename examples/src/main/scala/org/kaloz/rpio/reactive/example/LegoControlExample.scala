package org.kaloz.rpio.reactive.example

import java.util.concurrent.CountDownLatch
import java.util.logging.{Level, Logger}

import org.jnativehook.GlobalScreen
import org.jnativehook.keyboard.NativeKeyEvent._
import org.jnativehook.keyboard.{NativeKeyEvent, NativeKeyListener}
import org.kaloz.rpio.reactive.domain.DomainApi.Event
import org.kaloz.rpio.reactive.domain.GpioBoard
import org.kaloz.rpio.reactive.domain.GpioBoard._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel
import rx.lang.scala.Observer
import rx.lang.scala.subjects.PublishSubject

import scalaz._

object LegoControlExample extends App {

  val countDownLatch = new CountDownLatch(1)

  new LegoControlExample(countDownLatch)

  countDownLatch.await()
}

class LegoControlExample(countDownLatch: CountDownLatch) extends NativeKeyListener {

  // Get the logger for "org.jnativehook" and set the level to off.
  val logGlobalScreen = Logger.getLogger(classOf[GlobalScreen].getPackage().getName())
  logGlobalScreen.setLevel(Level.OFF)
  // Don't forget to disable the parent handlers.
  logGlobalScreen.setUseParentHandlers(false)
  GlobalScreen.registerNativeHook()

  implicit val protocolHandlerFactory = PiGpioSocketChannel
  implicit val subject = PublishSubject[Event]()

  val subscription = subject.subscribe(Observer[Event]((event: Event) => println(event)))

  val state = scalaz.StateT.stateMonad[GpioBoard]

  def initBoard(): State[GpioBoard, Unit] = for {
    _ <- provisionDefaultGpioOutputPins(25, 16, 21, 23, 24, 12, 18)
    _ <- writeValue(25, High)
  } yield {}

  var board = initBoard run (new GpioBoard())
  var up, down, left, right: Boolean = false

  GlobalScreen.addNativeKeyListener(this)

  def nativeKeyPressed(e: NativeKeyEvent): Unit = {
    println(s"Key pressed: ${getKeyText(e.getKeyCode())}")

    if (!up && "Up" == getKeyText(e.getKeyCode())) {
      def change(): State[GpioBoard, Unit] = for {
        _ <- writeValue(16, High)
        _ <- writeValue(21, Low)
        _ <- writeValue(12, High)
      } yield {
        up = true
      }
      board = change run (board._1)
    }
    else if (!down && "Down" == getKeyText(e.getKeyCode())) {
      def change(): State[GpioBoard, Unit] = for {
        _ <- writeValue(16, Low)
        _ <- writeValue(21, High)
        _ <- writeValue(12, High)
      } yield {
        down = true
      }
      board = change run (board._1)
    }
    else if (!right && "Right" == getKeyText(e.getKeyCode())) {
      def change(): State[GpioBoard, Unit] = for {
        _ <- writeValue(18, High)
        _ <- writeValue(23, Low)
        _ <- writeValue(24, High)
      } yield {
        right = true
      }
      board = change run (board._1)
    }
    else if (!left && "Left" == getKeyText(e.getKeyCode())) {
      def change(): State[GpioBoard, Unit] = for {
        _ <- writeValue(18, High)
        _ <- writeValue(23, High)
        _ <- writeValue(24, Low)
      } yield {
        left = true
      }
      board = change run (board._1)
    }
    else if ('q' == getKeyText(e.getKeyCode()).charAt(0).toLower) {
      GlobalScreen.removeNativeKeyListener(this)
      GlobalScreen.unregisterNativeHook()
      board = shutdown() run (board._1)
      println(board)
      countDownLatch.countDown()
    }
  }

  def nativeKeyReleased(e: NativeKeyEvent): Unit = {
    println(s"Key released: ${getKeyText(e.getKeyCode())}")

    if ((up && "Up" == getKeyText(e.getKeyCode())) || (down && "Down" == getKeyText(e.getKeyCode()))) {
      def change(): State[GpioBoard, Unit] = for {
        _ <- writeValue(16, Low)
        _ <- writeValue(21, Low)
        _ <- writeValue(12, Low)
      } yield {
        up = false; down = false
      }
      board = change run (board._1)
    }
    else if ((right && "Right" == getKeyText(e.getKeyCode())) || (left && "Left" == getKeyText(e.getKeyCode()))) {
      def change(): State[GpioBoard, Unit] = for {
        _ <- writeValue(18, Low)
        _ <- writeValue(23, Low)
        _ <- writeValue(24, Low)
      } yield {
        left = false; right = false
      }
      board = change run (board._1)
    }
  }

  def nativeKeyTyped(e: NativeKeyEvent) {}

}
