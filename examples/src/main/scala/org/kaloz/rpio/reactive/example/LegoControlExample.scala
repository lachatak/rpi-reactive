package org.kaloz.rpio.reactive.example

//import java.util.concurrent.CountDownLatch
//import java.util.logging.{Level, Logger}
//
//import org.jnativehook.GlobalScreen
//import org.jnativehook.keyboard.NativeKeyEvent._
//import org.jnativehook.keyboard.{NativeKeyEvent, NativeKeyListener}
//import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi._
//import org.kaloz.rpio.reactive.domain.PinMode._
//import org.kaloz.rpio.reactive.domain.PinValue.PinDigitalValue._
//import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PiGpioSocketChannel$
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future

object LegoControlExample extends App {

//  val countDownLatch = new CountDownLatch(1)
//
//  new LegoControlExample(countDownLatch)
//
//  countDownLatch.await()
}

//class LegoControlExample(countDownLatch: CountDownLatch) extends NativeKeyListener {

//  // Get the logger for "org.jnativehook" and set the level to off.
//  val logGlobalScreen = Logger.getLogger(classOf[GlobalScreen].getPackage().getName())
//  logGlobalScreen.setLevel(Level.OFF)
//  // Don't forget to disable the parent handlers.
//  logGlobalScreen.setUseParentHandlers(false)
//  GlobalScreen.registerNativeHook()
//
//  GlobalScreen.addNativeKeyListener(this)
//
//  val init = for {
//    controller <- PiGpioSocketChannel()
//    _ <- controller.request(ChangeModeRequest(18, Output))
//    _ <- controller.request(ChangeModeRequest(23, Output))
//    _ <- controller.request(ChangeModeRequest(24, Output))
//    _ <- controller.request(ChangeModeRequest(25, Output))
//    _ <- controller.request(ChangeModeRequest(16, Output))
//    _ <- controller.request(ChangeModeRequest(21, Output))
//    _ <- controller.request(ChangeModeRequest(12, Output))
//    _ <- controller.request(WriteDigitalValueRequest(25, High))
//    close <- controller.close()
//  } yield close
//
//  init.mapTo[Unit].onSuccess { case _ => println("Initialised...") }
//
//  def nativeKeyPressed(e: NativeKeyEvent): Unit = {
//    println(s"Key pressed: ${getKeyText(e.getKeyCode())}")
//
//    if ("Up" == getKeyText(e.getKeyCode())) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(16, High))
//        _ <- controller.request(WriteDigitalValueRequest(21, Low))
//        _ <- controller.request(WriteDigitalValueRequest(12, High))
//        close <- controller.close()
//      } yield close
//    }
//    else if ("Down" == getKeyText(e.getKeyCode())) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(16, Low))
//        _ <- controller.request(WriteDigitalValueRequest(21, High))
//        _ <- controller.request(WriteDigitalValueRequest(12, High))
//        close <- controller.close()
//      } yield close
//    }
//    else if ("Right" == getKeyText(e.getKeyCode())) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(18, High))
//        _ <- controller.request(WriteDigitalValueRequest(23, Low))
//        _ <- controller.request(WriteDigitalValueRequest(24, High))
//        close <- controller.close()
//      } yield close
//    }
//    else if ("Left" == getKeyText(e.getKeyCode())) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(18, High))
//        _ <- controller.request(WriteDigitalValueRequest(23, High))
//        _ <- controller.request(WriteDigitalValueRequest(24, Low))
//        close <- controller.close()
//      } yield close
//    }
//    else if ('q' == getKeyText(e.getKeyCode()).charAt(0).toLower) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(25, Low))
//        _ <- Future {
//          GlobalScreen.removeNativeKeyListener(this)
//          GlobalScreen.unregisterNativeHook()
//          countDownLatch.countDown()
//        }
//        close <- controller.close()
//      } yield close
//    }
//  }
//
//  def nativeKeyReleased(e: NativeKeyEvent): Unit = {
//    println(s"Key released: ${getKeyText(e.getKeyCode())}")
//
//    if ("Up" == getKeyText(e.getKeyCode())) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(16, Low))
//        _ <- controller.request(WriteDigitalValueRequest(21, Low))
//        _ <- controller.request(WriteDigitalValueRequest(12, Low))
//        close <- controller.close()
//      } yield close
//    }
//    else if ("Down" == getKeyText(e.getKeyCode())) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(16, Low))
//        _ <- controller.request(WriteDigitalValueRequest(21, Low))
//        _ <- controller.request(WriteDigitalValueRequest(12, Low))
//        close <- controller.close()
//      } yield close
//    }
//    else if ("Right" == getKeyText(e.getKeyCode())) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(18, Low))
//        _ <- controller.request(WriteDigitalValueRequest(23, Low))
//        _ <- controller.request(WriteDigitalValueRequest(24, Low))
//        close <- controller.close()
//      } yield close
//    }
//    else if ("Left" == getKeyText(e.getKeyCode())) {
//      for {
//        controller <- PiGpioSocketChannel()
//        _ <- controller.request(WriteDigitalValueRequest(18, Low))
//        _ <- controller.request(WriteDigitalValueRequest(23, Low))
//        _ <- controller.request(WriteDigitalValueRequest(24, Low))
//        close <- controller.close()
//      } yield close
//    }
//  }
//
//  def nativeKeyTyped(e: NativeKeyEvent) {}

//}
