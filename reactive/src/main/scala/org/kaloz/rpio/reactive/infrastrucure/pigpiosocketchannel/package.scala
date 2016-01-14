package org.kaloz.rpio.reactive.infrastrucure

import java.nio.{ByteBuffer, ByteOrder}

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.DomainApi
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.ChangeMode.ChangeModeRequest
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.ChangePud.ChangePudRequest
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.ReadDigitalValue.ReadDigitalValueRequest
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.WriteDigitalValue.WriteDigitalValueRequest
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.WritePwmValue.WritePwmValueRequest


package object pigpiosocketchannel extends StrictLogging {

  object InfrastructureApi {

    object PiGpioSocketChannelRequest {
      implicit def domainToInfrastructure(request: DomainApi.Request): PiGpioSocketChannelRequest = request match {
        case DomainApi.ChangeModeRequest(pin, pinMode) => ChangeModeRequest(pin, pinMode)
        case DomainApi.ChangePudRequest(pin, pudMode) => ChangePudRequest(pin, pudMode)
        case DomainApi.ReadValueRequest(pin) => ReadDigitalValueRequest(pin)
        case DomainApi.WriteValueRequest(pin, value) => value match {
          case Low | High => WriteDigitalValueRequest(pin, value)
          case dutyCycle@Pwm(_) => WritePwmValueRequest(pin, dutyCycle)
        }
      }
    }

    sealed trait PiGpioSocketChannelRequest extends DomainApi.Request {
      type ResponseHandlerType <: ResponseHandler

      def command: Int

      def param1: Int

      def param2: Int

      def param3: Int

      def fill(writeBuffer: ByteBuffer)(implicit pin:Option[Int], uuid:String) = {
        writeBuffer.putInt(command).putInt(param1).putInt(param2).putInt(param3).flip()
        logger.info(s"Request - $pin:$uuid:${writeBuffer.array().map("%02x".format(_)).mkString(" ")}")
        writeBuffer
      }

      def responseHandler: ResponseHandlerType
    }

    sealed abstract class ResponseHandler() {
      val readBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)

      def responseValue(implicit pin:Option[Int], uuid:String) = {
        readBuffer.rewind()
        logger.info(s"Response - $pin:$uuid:${readBuffer.array().map("%02x".format(_)).mkString(" ")}")
        readBuffer.asIntBuffer().get(3)
      }

      def response(implicit pin:Option[Int], uuid:String): DomainApi.Response
    }

    object ChangeMode {

      case class ChangeModeRequest(pin: Int, value: PinMode) extends PiGpioSocketChannelRequest {
        type ResponseHandlerType = ChangeModeResponseHandler

        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PinMode.pinModeToInt

        val command: Int = 0
        val param1: Int = pin
        val param2: Int = value
        val param3: Int = 0

        val responseHandler = ChangeModeResponseHandler()
      }

      case class ChangeModeResponseHandler() extends ResponseHandler {
        def response(implicit pin:Option[Int], uuid:String) = DomainApi.ChangeModeResponse(responseValue)
      }

    }

    object ChangePud {

      case class ChangePudRequest(pin: Int, value: PudMode) extends PiGpioSocketChannelRequest {
        type ResponseHandlerType = ChangePudResponseHandler

        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PudMode.pudModeToInt

        val command: Int = 2
        val param1: Int = pin
        val param2: Int = value
        val param3: Int = 0

        val responseHandler = ChangePudResponseHandler()
      }

      case class ChangePudResponseHandler() extends ResponseHandler {
        def response(implicit pin:Option[Int], uuid:String) = DomainApi.ChangePudResponse(responseValue)
      }

    }

    object ReadDigitalValue {

      case class ReadDigitalValueRequest(pin: Int) extends PiGpioSocketChannelRequest {
        type ResponseHandlerType = ReadDigitalValueResponseHandler

        val command: Int = 3
        val param1: Int = pin
        val param2: Int = 0
        val param3: Int = 0

        val responseHandler = ReadDigitalValueResponseHandler()
      }

      case class ReadDigitalValueResponseHandler() extends ResponseHandler {

        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PinValue.intToPinValue

        def response(implicit pin:Option[Int], uuid:String) = DomainApi.ReadValueResponse(responseValue)
      }

    }

    object WriteDigitalValue {

      case class WriteDigitalValueRequest(pin: Int, value: PinValue) extends PiGpioSocketChannelRequest {
        type ResponseHandlerType = WriteDigitalValueResponseHandler

        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PinValue.pinValueToInt

        val command: Int = 4
        val param1: Int = pin
        val param2: Int = value
        val param3: Int = 0

        val responseHandler = WriteDigitalValueResponseHandler()
      }

      case class WriteDigitalValueResponseHandler() extends ResponseHandler {
        def response(implicit pin:Option[Int], uuid:String) = DomainApi.WriteValueResponse(responseValue)
      }

    }

    object WritePwmValue {

      case class WritePwmValueRequest(pin: Int, dutyCycle: Pwm) extends PiGpioSocketChannelRequest {
        type ResponseHandlerType = WritePwmResponseHandler

        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PinValue.pinPwmValueToInt

        val command: Int = 5
        val param1: Int = pin
        val param2: Int = dutyCycle
        val param3: Int = 0

        val responseHandler = WritePwmResponseHandler()
      }

      case class WritePwmResponseHandler() extends ResponseHandler {
        def response(implicit pin:Option[Int], uuid:String) = DomainApi.WriteValueResponse(responseValue)
      }

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

    implicit def charToPudMode(pudMode: Char): PudMode = pudMode match {
      case 'O' => PudOff
      case 'U' => PudUp
      case 'D' => PudDown
      case x: Char => throw new NotImplementedError(s"$x pudMode is not implemented!!")
    }

    implicit def pudModeToInt(pudMode: PudMode): Int = pudMode match {
      case PudOff => 'O'.toInt
      case PudUp => 'U'.toInt
      case PudDown => 'D'.toInt
    }
  }

}
