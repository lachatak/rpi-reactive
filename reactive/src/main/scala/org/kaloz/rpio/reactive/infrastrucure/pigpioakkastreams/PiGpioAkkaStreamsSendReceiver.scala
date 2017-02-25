package org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.api.{DomainRequest, Mapper, SendReceiver, Valid}
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.GpioAssembler._
import shapeless.the

case class PiGpioAkkaStreamsSendReceiver(piGpioAkkaStreamsClient: PiGpioAkkaStreamsClient) extends StrictLogging with SendReceiver {

  override def sendReceive[A <: DomainRequest, R](request: A)(implicit mapper: Mapper.Aux[A, R]): Valid[R] = {
//    the[GpioAssembler[A, R]]
    println(mapper)
    sendReceive2(request)
    "af".asInstanceOf[Valid[R]]
  }

  private def sendReceive2[A <: DomainRequest, R](request: A)(implicit  mapper: Mapper.Aux[A, R], assembler: GpioAssembler[A, R]): Valid[R] = {
    piGpioAkkaStreamsClient.sendReceive(assembler.disassemble(request))
      .map(response => assembler.assemble(response))
  }

}
