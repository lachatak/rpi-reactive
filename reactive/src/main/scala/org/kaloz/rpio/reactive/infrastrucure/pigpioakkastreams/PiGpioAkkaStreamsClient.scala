package org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams

import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Keep, Sink, Source, Tcp}
import akka.stream.{ActorMaterializer, Attributes, KillSwitches, OverflowStrategy}
import com.typesafe.scalalogging.StrictLogging
import monix.eval.Task
import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.ResponseProtocol.{GpioResponse, NotificationResponse}
import org.reactivestreams.Publisher
import scodec.interop.akka._
import scodec.{Codec, Decoder}

import scala.concurrent.Future

case class PiGpioAkkaStreamsClient()(implicit context: ActorSystem, materializer: ActorMaterializer) extends StrictLogging with Configuration {

  private val ((requestQueue, killSwitch), responsePublisher) =
    Source.queue[GpioRequest](1000, OverflowStrategy.fail)
      .viaMat(KillSwitches.single)(Keep.both)
      .map(Codec.encode(_).require.toByteVector.toByteString)
      .log("request").withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
      .via(Tcp().outgoingConnection(new InetSocketAddress(PigpioServerConf.serverHost, PigpioServerConf.serverPort)))
      .log("reply").withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
      .map(byteString => Decoder.decodeCollect[List, ResponseProtocol](ResponseProtocol.decoder, None)(byteString.toByteVector.bits).require.value)
      .mapConcat(identity)
      .toMat(Sink.asPublisher(true))(Keep.both).run

  private val responseSource: Source[ResponseProtocol, NotUsed] = Source.fromPublisher(responsePublisher)

  private val gpioResponsePublisher: Publisher[GpioResponse] =
    responseSource.collect { case r: GpioResponse => r }.toMat(Sink.asPublisher(true))(Keep.right).run()

  val gpioNotificationResponsePublisher: Publisher[NotificationResponse] =
    responseSource.collect { case n: NotificationResponse => n }.toMat(Sink.asPublisher(true))(Keep.right).run()

  Source.fromPublisher(gpioResponsePublisher).runWith(Sink.ignore)

  private val receiver = (request: GpioRequest) =>
    Source.fromPublisher(gpioResponsePublisher)
      .filter(_ belongsTo request)
      .toMat(Sink.head)(Keep.right)

  def sendReceive(request: GpioRequest): Task[GpioResponse] = {
    requestQueue.offer(request)
    Task.deferFuture(receiver(request).run)
  }

  def stop(): Unit = killSwitch.shutdown()
}
