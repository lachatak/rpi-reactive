package org.kaloz.rpio.reactive.infrastrucure

import cats.data.NonEmptyList
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.Protocol.GpioResponse
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.Protocol.NotificationType.Change
import scodec.bits.{BitVector, _}
import scodec.codecs.{Discriminated, conditional, constant, fixedSizeBytes, ignore, int32L, mappedEnum, uint8L, uintL, _}
import scodec.{Codec, Decoder, _}

import scala.concurrent.Future

package object pigpioakkastreams {

  type GpioRespone = Future[Either[NonEmptyList[String], GpioResponse]]

  sealed trait Command

  case object MODES extends Command

  case object PUD extends Command

  case object READ extends Command

  case object WRITE extends Command

  case object PWM extends Command

  case object WDOG extends Command

  case object BR1 extends Command

  case object NB extends Command

  case object NC extends Command

  case object NOIB extends Command

  case object HWVER extends Command

  object Command {

    implicit val discriminated: Discriminated[Command, Int] = Discriminated(uint8L)
    implicit val codec: Codec[Command] = mappedEnum(uint8L,
      MODES -> 0,
      PUD -> 2,
      READ -> 3,
      WRITE -> 4,
      PWM -> 5,
      WDOG -> 9,
      BR1 -> 10,
      HWVER -> 17,
      NB -> 19,
      NC -> 21,
      NOIB -> 99
    )
  }

  case class GpioRequest(command: Command, p1: Option[Int] = None, p2: Option[Int] = None)

  object GpioRequest {
    implicit val codec: Codec[GpioRequest] = fixedSizeBytes(16, {
      (("command" | Codec[Command]) <~
        ("fillers" | constant(BitVector.fill(24)(false)))) ::
        ("p1" | conditional(true, int32L)) ::
        ("p2" | conditional(true, int32L)) ::
        ("p3" | constant(BitVector.fill(32)(false)))
    }).as[GpioRequest]
  }

  sealed trait Protocol

  object Protocol {

    case class GpioResponse(commandId: Command, result: Int) extends Protocol

    object GpioResponse {
      val decoder: Decoder[GpioResponse] = fixedSizeBytes(16, {
        (("command" | Codec[Command]) <~
          ("fillers" | constant(BitVector.fill(24)(false)))) ::
          ("ignore" | ignore(64)) :~>:
          ("result" | int32L)
      }).as[GpioResponse]
    }

    sealed trait NotificationType

    object NotificationType {

      case object Change extends NotificationType {
        implicit val codec: Codec[Change.type] = fixedSizeBytes(2, (constant(hex"0x0000".bits)).hlist.dropUnits).as[Change.type]
      }

      case class WDog(timeoutPin: Int) extends NotificationType

      object WDog {
        implicit val codec: Codec[WDog] = fixedSizeBytes(2, {
          constant(bin"001") ~> uintL(5) <~ constant(hex"0x00".bits)
        }).as[WDog]
      }

      case object Alive extends NotificationType {
        implicit val codec: Codec[Alive.type] = fixedSizeBytes(2, (constant(hex"0x4000".bits)).hlist.dropUnits.as[Alive.type])
      }

      case class Event(eventBits: Int) extends NotificationType

      object Event {
        implicit val coder: Codec[Event] = fixedSizeBytes(2, {
          ("event" | constant(bin"100")) ~>
            ("eventBits" | uintL(5)) <~
            ("drop" | constant(hex"0x00".bits))
        }).as[Event]
      }

      implicit val codec: Codec[NotificationType] = Codec.coproduct[NotificationType].choice
    }

    case class NotificationResponse(notificationType: NotificationType, level: Option[Int]) extends Protocol

    object NotificationResponse {
      val codec: Codec[NotificationResponse] = fixedSizeBytes(12, {
        ("seq" | ignore(16)) ~>
          ("type" | Codec[NotificationType])  >>:~ { nType =>
          ("tick" | ignore(32)) :~>:
            ("level" | conditional(nType.isInstanceOf[Change.type], int32L))
        }}).as[NotificationResponse]
    }

    val decoder: Decoder[Protocol] = Decoder.choiceDecoder(GpioResponse.decoder, NotificationResponse.codec.asDecoder)
  }
}
