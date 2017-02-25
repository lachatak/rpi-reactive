package org.kaloz.rpio.reactive.infrastrucure

import org.kaloz.rpio.reactive.domain.ChannelHandler
import org.kaloz.rpio.reactive.domain.PinMode.{Input, Output, PinMode}
import org.kaloz.rpio.reactive.domain.PinValue.{High, Low, PinValue, Pwm}
import org.kaloz.rpio.reactive.domain.PudMode.{PudDown, PudMode, PudOff, PudUp}
import org.kaloz.rpio.reactive.domain.api.{ChangePinModeRequest, ChangePudModeRequest, CloseNotificationChannelRequest, DomainRequest, OpenNotificationChannelRequest, OpenNotificationChannelResponse, ReadAllPinValuesRequest, ReadAllPinValuesResponse, ReadValueRequest, ReadValueResponse, SubscribeNotificationRequest, VersionRequest, VersionResponse, WriteValueRequest}
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.ResponseProtocol.GpioResponse
import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.ResponseProtocol.NotificationType.Change
import scodec.bits.{BitVector, _}
import scodec.codecs.{Discriminated, conditional, constant, fixedSizeBytes, ignore, int32L, mappedEnum, uint8L, uintL, _}
import scodec.{Codec, Decoder, _}

package object pigpioakkastreams {

  sealed trait Notification

  sealed trait NoResult

  sealed trait Command

  case object MODES extends Command with NoResult

  case object PUD extends Command with NoResult

  case object READ extends Command

  case object WRITE extends Command with NoResult

  case object PWM extends Command with NoResult

  case object WDOG extends Command with NoResult

  case object BR1 extends Command

  case object NB extends Command with Notification with NoResult

  case object NC extends Command with Notification with NoResult

  case object NOIB extends Command with Notification

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

  case class GpioRequest(command: Command, p1: Int = 0, p2: Int = 0)

  object GpioRequest {
    implicit val codec: Codec[GpioRequest] = fixedSizeBytes(16, {
      (("command" | Codec[Command]) <~
        ("fillers" | constant(BitVector.fill(24)(false)))) ::
        ("p1" | int32L) ::
        ("p2" | int32L) ::
        ("p3" | constant(BitVector.fill(32)(false)))
    }).as[GpioRequest]
  }

  sealed trait ResponseProtocol

  object ResponseProtocol {

    case class GpioResponse(command: Command, p1: Int = 0, p2: Int = 0, p3: Int) extends ResponseProtocol {
      def belongsTo(request: GpioRequest): Boolean = command == request.command && p1 == request.p1 && p2 == request.p2
    }

    object GpioResponse {
      val decoder: Decoder[GpioResponse] = fixedSizeBytes(16, {
        (("command" | Codec[Command]) <~
          ("fillers" | constant(BitVector.fill(24)(false)))) ::
          ("p1" | int32L) ::
          ("p2" | int32L) ::
          ("p3" | int32L)
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

    case class NotificationResponse(notificationType: NotificationType, level: Option[Int]) extends ResponseProtocol

    object NotificationResponse {
      val codec: Codec[NotificationResponse] = fixedSizeBytes(12, {
        ("seq" | ignore(16)) ~>
          ("type" | Codec[NotificationType]) >>:~ { nType =>
          ("tick" | ignore(32)) :~>:
            ("level" | conditional(nType.isInstanceOf[Change.type], int32L))
        }
      }).as[NotificationResponse]
    }

    val decoder: Decoder[ResponseProtocol] = Decoder.choiceDecoder(GpioResponse.decoder, NotificationResponse.codec.asDecoder)
  }


  trait GpioAssembler[A <: DomainRequest, R] {

    protected def defaultResponse(response: GpioResponse) = response.p3 match {
      case 0 => Right(())
      case _ => Left(s"Error processing request $response")
    }

    def disassemble(request: A): GpioRequest

    def assemble(response: GpioResponse): Either[String, R]
  }

  object GpioAssembler {

    def apply[A0 <: DomainRequest, R0](implicit assembler: GpioAssembler[A0, R0]) = assembler

    implicit val changePinModeRequestAssembler = new GpioAssembler[ChangePinModeRequest, Unit] {

      override def disassemble(out: ChangePinModeRequest): GpioRequest = {
        import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.PinMode.pinModeToInt
        GpioRequest(MODES, out.pinNumber.id, out.pinMode)
      }

      override def assemble(in: GpioResponse): Either[String, Unit] = defaultResponse(in)
    }

    implicit val changePudModeRequestAssembler = new GpioAssembler[ChangePudModeRequest, Unit] {

      override def disassemble(out: ChangePudModeRequest): GpioRequest = {
        import org.kaloz.rpio.reactive.infrastrucure.pigpioakkastreams.PudMode.pudModeToInt
        GpioRequest(PUD, out.pinNumber.id, out.pudMode)
      }

      override def assemble(in: GpioResponse): Either[String, Unit] = defaultResponse(in)
    }

    implicit val readValueRequestAssembler = new GpioAssembler[ReadValueRequest, ReadValueResponse] {

      override def disassemble(out: ReadValueRequest): GpioRequest = GpioRequest(READ, out.pinNumber.id)

      override def assemble(in: GpioResponse): Either[String, ReadValueResponse] = Right(ReadValueResponse(in.p3))
    }

    implicit val writeValueRequestAssembler = new GpioAssembler[WriteValueRequest, Unit] {

      override def disassemble(out: WriteValueRequest): GpioRequest = out.value match {
        case Low | High => GpioRequest(WRITE, out.pinNumber.id, out.value)
        case dutyCycle@Pwm(_) => GpioRequest(PWM, out.pinNumber.id, dutyCycle)
      }

      override def assemble(in: GpioResponse): Either[String, Unit] = defaultResponse(in)
    }

    implicit val readAllPinValuesRequestAssembler = new GpioAssembler[ReadAllPinValuesRequest, ReadAllPinValuesResponse] {

      override def disassemble(out: ReadAllPinValuesRequest): GpioRequest = GpioRequest(BR1)

      override def assemble(in: GpioResponse): Either[String, ReadAllPinValuesResponse] = Right(ReadAllPinValuesResponse(in.p3))
    }

    implicit val subscribeNotificationRequestAssembler = new GpioAssembler[SubscribeNotificationRequest, Unit] {

      override def disassemble(out: SubscribeNotificationRequest): GpioRequest = GpioRequest(NB, out.handler.id, 1 << out.pin.id)

      override def assemble(in: GpioResponse): Either[String, Unit] = defaultResponse(in)
    }

    implicit val closeNotificationChannelRequestAssembler = new GpioAssembler[CloseNotificationChannelRequest, Unit] {

      override def disassemble(out: CloseNotificationChannelRequest): GpioRequest = GpioRequest(NC, out.handler.id)

      override def assemble(in: GpioResponse): Either[String, Unit] = defaultResponse(in)
    }

    implicit val openNotificationChannelRequestAssembler = new GpioAssembler[OpenNotificationChannelRequest, OpenNotificationChannelResponse] {

      override def disassemble(out: OpenNotificationChannelRequest): GpioRequest = GpioRequest(NOIB)

      override def assemble(in: GpioResponse): Either[String, OpenNotificationChannelResponse] = Right(OpenNotificationChannelResponse(ChannelHandler(in.p3)))
    }

    implicit val versionRequestAssembler = new GpioAssembler[VersionRequest, VersionResponse] {

      override def disassemble(out: VersionRequest): GpioRequest = GpioRequest(HWVER)

      override def assemble(in: GpioResponse): Either[String, VersionResponse] = Right(VersionResponse(in.p3))
    }
  }

  object PinMode {

    implicit def intToPinMode(pinMode: Int): PinMode = pinMode match {
      case 0 => Input
      case 1 => Output
      case x: Int => throw new NotImplementedError(s"$x pinMode is not implemented!!")
    }

    implicit def pinModeToInt(pinMode: PinMode): Int = pinMode match {
      case Input => 0
      case Output => 1
    }
  }

  object PinValue {

    implicit def intToPinValue(pinValue: Int): PinValue = pinValue match {
      case 0 => Low
      case 1 => High
      case x => Pwm(x)
    }

    implicit def pinValueToInt(pinValue: PinValue): Int = pinValue match {
      case Low => 0
      case High => 1
      case Pwm(x) => x
    }

    implicit def pinPwmValueToInt(pinValue: Pwm): Int = pinValue.value

    implicit def intToPinPwmValue(pinValue: Int): Int = Pwm(pinValue)
  }

  object PudMode {

    implicit def charToPudMode(pudMode: Int): PudMode = pudMode match {
      case 0 => PudOff
      case 1 => PudDown
      case 2 => PudUp
      case x => throw new NotImplementedError(s"$x pudMode is not implemented!!")
    }

    implicit def pudModeToInt(pudMode: PudMode): Int = pudMode match {
      case PudOff => 0
      case PudDown => 1
      case PudUp => 2
    }
  }

}