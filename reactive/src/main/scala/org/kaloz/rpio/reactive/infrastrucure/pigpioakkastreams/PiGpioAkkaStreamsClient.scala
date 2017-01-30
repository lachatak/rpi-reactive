package org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams

import java.net.InetSocketAddress

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Keep, Merge, Sink, Source, Tcp}
import akka.stream.{ActorMaterializer, Attributes, KillSwitches, OverflowStrategy}
import cats.data.NonEmptyList
import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.Protocol.{GpioResponse, NotificationResponse}
import org.reactivestreams.Publisher
import scodec.interop.akka._
import scodec.{Codec, Decoder}

case class PiGpioAkkaStreamsClient()(implicit context: ActorSystem, materializer: ActorMaterializer) extends StrictLogging with Configuration {

  private val ((notificationQueue, notificationKillSwitch), notificationPublisher) = piGpioFlow(Protocol.decoder).run()
  private val ((controlQueue, controlKillSwitch), controlPublisher) = piGpioFlow(GpioResponse.decoder).run()

  private val controlSource: Source[GpioResponse, NotUsed] = Source.fromPublisher(controlPublisher)
  private val notificationSource: Source[Protocol, NotUsed] = Source.fromPublisher(notificationPublisher)

  val publisher = notificationSource.collect { case n: NotificationResponse => n }.toMat(Sink.asPublisher(true))(Keep.right).run()

  private val responseQueue: Publisher[GpioResponse] =
    Source.combine(notificationSource.collect { case r: GpioResponse => r }, controlSource)(Merge(_)).toMat(Sink.asPublisher(true))(Keep.right).run()

  Source.fromPublisher(responseQueue).runWith(Sink.ignore)

  private def piGpioFlow[A](decoder: Decoder[A]) =
    Source.queue[GpioRequest](1000, OverflowStrategy.fail)
      .viaMat(KillSwitches.single)(Keep.both)
      .map(Codec.encode(_).require.toByteVector.toByteString)
      .log("request").withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
      .via(Tcp().outgoingConnection(new InetSocketAddress(PigpioServerConf.serverHost, PigpioServerConf.serverPort)))
      .log("reply").withAttributes(Attributes.logLevels(onElement = Logging.DebugLevel))
      .map(byteString => Decoder.decodeCollect[List, A](decoder, None)(byteString.toByteVector.bits).require.value)
      .mapConcat(identity)
      .toMat(Sink.asPublisher(true))(Keep.both)

  private val receiver = (request: GpioRequest) =>
    Source.fromPublisher(responseQueue)
      .filter(x => x.command == request.command && x.p1 == request.p1 && x.p2 == request.p2)
      .map {
        case r@GpioResponse(c, _, _, p3) if (c.isInstanceOf[NoResult] && p3 < 0) => Left(NonEmptyList.of(s"Response $r is not valid for $request"))
        case x => Right(x)
      }
      .toMat(Sink.head)(Keep.right)

  def sendReceive(request: GpioRequest): GpioRespone = {
    request match {
      case r@GpioRequest(c, _, _) if c.isInstanceOf[Notification] => notificationQueue.offer(r)
      case r => controlQueue.offer(r)
    }
    receiver(request).run
  }

  def stop(): Unit = {
    controlKillSwitch.shutdown()
    notificationKillSwitch.shutdown()
  }
}
