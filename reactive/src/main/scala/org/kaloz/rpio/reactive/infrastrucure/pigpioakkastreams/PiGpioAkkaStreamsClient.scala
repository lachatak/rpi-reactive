package org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams

import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Merge, Sink, SinkQueueWithCancel, Source, Tcp}
import akka.stream.{ActorMaterializer, KillSwitches, OverflowStrategy}
import cats.data.{NonEmptyList, OptionT}
import cats.instances.future._
import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.Protocol.{GpioResponse, NotificationResponse}
import scodec.interop.akka._
import scodec.{Codec, Decoder}

import scala.concurrent.ExecutionContext.Implicits.global

case class PiGpioAkkaStreamsClient()(implicit context: ActorSystem, materializer: ActorMaterializer) extends StrictLogging with Configuration {

  private val ((notificationQueue, notificationKillSwitch), notificationPublisher) = piGpioFlow(Protocol.decoder).run()
  private val ((controlQueue, controlKillSwitch), controlPublisher) = piGpioFlow(GpioResponse.decoder).run()

  private val controlSource: Source[GpioResponse, NotUsed] = Source.fromPublisher(controlPublisher)
  private val notificationSource: Source[Protocol, NotUsed] = Source.fromPublisher(notificationPublisher)

  val publisher = notificationSource.collect { case n: NotificationResponse => n }.toMat(Sink.asPublisher(true))(Keep.right).run()

  private val responseQueue: SinkQueueWithCancel[GpioResponse] =
    Source.combine(notificationSource.collect { case r: GpioResponse => r }, controlSource)(Merge(_)).toMat(Sink.queue())(Keep.right).run()

  private def piGpioFlow[A](decoder: Decoder[A]) =
    Source.queue[GpioRequest](1000, OverflowStrategy.fail)
      .viaMat(KillSwitches.single)(Keep.both)
      .map(Codec.encode(_).require.toByteVector.toByteString)
      .via(Tcp().outgoingConnection(new InetSocketAddress(PigpioServerConf.serverHost, PigpioServerConf.serverPort)))
      .map(byteString => Decoder.decodeCollect[List, A](decoder, None)(byteString.toByteVector.bits).require.value)
      .mapConcat(identity)
      .toMat(Sink.asPublisher(true))(Keep.both)

  def sendReceive(request: GpioRequest): GpioRespone = {
    request match {
      case r@GpioRequest(NB | NOIB | NC, _, _) => notificationQueue.offer(r)
      case r => controlQueue.offer(r)
    }
    OptionT(responseQueue.pull()).toRight(NonEmptyList.of(s"Response is not available for $request")).subflatMap {
      case r@GpioResponse(MODES | PUD | WRITE | PWM | WDOG | NC, result) if result < 0 => Left(NonEmptyList.of(s"Response $r is not valid for $request"))
      case x => Right(x)
    }.value
  }

  def stop(): Unit = {
    controlKillSwitch.shutdown()
    notificationKillSwitch.shutdown()
  }
}
