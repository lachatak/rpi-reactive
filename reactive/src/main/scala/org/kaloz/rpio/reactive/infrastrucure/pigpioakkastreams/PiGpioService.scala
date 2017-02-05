package org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.data.NonEmptyList
import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.api.{DomainRequest, DomainResponse, SendReceiveHandler, Valid}

case class PiGpioService(piGpioAkkaStreamsClient: PiGpioAkkaStreamsClient)(implicit context: ActorSystem, materializer: ActorMaterializer)
  extends StrictLogging
    with SendReceiveHandler {

  override def sendReceive[A <: DomainRequest, B <: DomainResponse](request: A): Valid[B] = {
    piGpioAkkaStreamsClient.sendReceive(domainToInfrastructureAssembler(request))
      .map(response => infrastructureToDomainAssembler(response).asInstanceOf[Either[NonEmptyList[String], B]])
  }
}
