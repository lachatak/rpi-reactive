package org.kaloz.rpio.reactive.infrastrucure

import java.nio.channels.SocketChannel
import java.nio.{ByteBuffer, ByteOrder}

import com.typesafe.scalalogging.StrictLogging
import org.kaloz.rpio.reactive.domain.api
import org.kaloz.rpio.reactive.domain.PinMode._
import org.kaloz.rpio.reactive.domain.PinValue._
import org.kaloz.rpio.reactive.domain.PudMode._
//import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.ChangeMode.ChangeModeRequest
//import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.ChangePud.ChangePudRequest
//import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.PiGpioVersion.PiGpioVersionRequest
//import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.ReadDigitalValue.ReadDigitalValueRequest
//import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.WriteDigitalValue.WriteDigitalValueRequest
//import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.WritePwmValue.WritePwmValueRequest

package object pigpiosocketchannel extends StrictLogging {

//  object InfrastructureApi {
//
//    object PiGpioSocketChannelRequest {
//      implicit def domainToInfrastructure(request: api.DomainRequest): PiGpioSocketChannelRequest = request match {
//        case api.ChangePinModeRequest(pinNumber, pinMode) => ChangeModeRequest(pinNumber, pinMode)
//        case api.ChangePudModeRequest(pinNumber, pudMode) => ChangePudRequest(pinNumber, pudMode)
//        case api.ReadValueRequest(pinNumber) => ReadDigitalValueRequest(pinNumber)
//        case api.WriteValueRequest(pinNumber, value) => value match {
//          case Low | High => WriteDigitalValueRequest(pinNumber, value)
//          case dutyCycle@Pwm(_) => WritePwmValueRequest(pinNumber, dutyCycle)
//        }
//        case api.VersionRequest() => PiGpioVersionRequest()
//      }
//    }
//
//    sealed trait PiGpioSocketChannelRequest extends api.DomainRequest {
//      type ResponseHandlerType <: ResponseHandler
//
//      def command: Int
//
//      def param1: Int
//
//      def param2: Int
//
//      def param3: Int
//
//      def fill(writeBuffer: ByteBuffer) = {
//        writeBuffer.clear()
//        writeBuffer.putInt(command).putInt(param1).putInt(param2).putInt(param3).flip()
//        logger.debug(s"Request - ${writeBuffer.array().map("%02x".format(_)).mkString(" ")}")
//        writeBuffer
//      }
//
//      def responseHandler: ResponseHandlerType
//
//      def domainResponse: SocketChannel => api.DomainResponse = responseHandler.response
//
//    }
//
//    sealed trait ResponseHandler {
//
//      val readResponse: SocketChannel => ByteBuffer = { socketChannel =>
//        val readBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
//        socketChannel.read(readBuffer)
//        readBuffer.rewind()
//        logger.debug(s"Response - ${readBuffer.array().map("%02x".format(_)).mkString(" ")}")
//        readBuffer
//      }
//
//      val fetchResponse: ByteBuffer => Int = byteBuffer => byteBuffer.asIntBuffer().get(3)
//
//      val convertResponse: Int => api.DomainResponse
//
//      def response = readResponse andThen fetchResponse andThen convertResponse
//    }
//
//    object ChangeMode {
//
//      case class ChangeModeRequest(pinNumber: Int, value: PinMode) extends PiGpioSocketChannelRequest {
//        type ResponseHandlerType = ChangeModeResponseHandler.type
//
//        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PinMode.pinModeToInt
//
//        val command: Int = 0
//        val param1: Int = pinNumber
//        val param2: Int = value
//        val param3: Int = 0
//
//        val responseHandler = ChangeModeResponseHandler
//      }
//
//      case object ChangeModeResponseHandler extends ResponseHandler {
//        val convertResponse: Int => api.ChangePinModeResponse = result => api.ChangePinModeResponse(result)
//      }
//
//    }
//
//    object ChangePud {
//
//      case class ChangePudRequest(pinNumber: Int, value: PudMode) extends PiGpioSocketChannelRequest {
//        type ResponseHandlerType = ChangePudResponseHandler
//
//        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PudMode.pudModeToInt
//
//        val command: Int = 2
//        val param1: Int = pinNumber
//        val param2: Int = value
//        val param3: Int = 0
//
//        val responseHandler = ChangePudResponseHandler()
//      }
//
//      case class ChangePudResponseHandler() extends ResponseHandler {
//        val convertResponse: Int => api.ChangePudModeResponse = result => api.ChangePudModeResponse(result)
//      }
//
//    }
//
//    object ReadDigitalValue {
//
//      case class ReadDigitalValueRequest(pinNumber: Int) extends PiGpioSocketChannelRequest {
//        type ResponseHandlerType = ReadDigitalValueResponseHandler
//
//        val command: Int = 3
//        val param1: Int = pinNumber
//        val param2: Int = 0
//        val param3: Int = 0
//
//        val responseHandler = ReadDigitalValueResponseHandler()
//      }
//
//      case class ReadDigitalValueResponseHandler() extends ResponseHandler {
//
//        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PinValue.intToPinValue
//
//        val convertResponse: Int => api.ReadValueResponse = result => api.ReadValueResponse(result)
//      }
//
//    }
//
//    object WriteDigitalValue {
//
//      case class WriteDigitalValueRequest(pinNumber: Int, value: PinValue) extends PiGpioSocketChannelRequest {
//        type ResponseHandlerType = WriteDigitalValueResponseHandler
//
//        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PinValue.pinValueToInt
//
//        val command: Int = 4
//        val param1: Int = pinNumber
//        val param2: Int = value
//        val param3: Int = 0
//
//        val responseHandler = WriteDigitalValueResponseHandler()
//      }
//
//      case class WriteDigitalValueResponseHandler() extends ResponseHandler {
//        val convertResponse: Int => api.WriteValueResponse = result => api.WriteValueResponse(result)
//      }
//
//    }
//
//    object WritePwmValue {
//
//      case class WritePwmValueRequest(pinNumber: Int, dutyCycle: Pwm) extends PiGpioSocketChannelRequest {
//        type ResponseHandlerType = WritePwmResponseHandler
//
//        import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.PinValue.pinPwmValueToInt
//
//        val command: Int = 5
//        val param1: Int = pinNumber
//        val param2: Int = dutyCycle
//        val param3: Int = 0
//
//        val responseHandler = WritePwmResponseHandler()
//      }
//
//      case class WritePwmResponseHandler() extends ResponseHandler {
//        val convertResponse: Int => api.WriteValueResponse = result => api.WriteValueResponse(result)
//      }
//
//    }
//
//    object PiGpioVersion {
//
//      case class PiGpioVersionRequest() extends PiGpioSocketChannelRequest {
//        type ResponseHandlerType = PiGpioVersionResponseHandler
//
//        val command: Int = 26
//        val param1: Int = 0
//        val param2: Int = 0
//        val param3: Int = 0
//
//        val responseHandler = PiGpioVersionResponseHandler()
//      }
//
//      case class PiGpioVersionResponseHandler() extends ResponseHandler {
//
//        val convertResponse: Int => api.VersionResponse = result => api.VersionResponse(result)
//      }
//
//    }
//
//  }
//
//  object PinMode {
//
//    implicit def intToPinMode(pinMode: Int): PinMode = pinMode match {
//      case 0 => Input
//      case 1 => Output
//      case x: Int => throw new NotImplementedError(s"$x pinMode is not implemented!!")
//    }
//
//    implicit def pinModeToInt(pinMode: PinMode): Int = pinMode match {
//      case Input => 0
//      case Output => 1
//    }
//  }
//
//  object PinValue {
//
//    implicit def intToPinValue(pinValue: Int): PinValue = pinValue match {
//      case 0 => Low
//      case 1 => High
//      case x => Pwm(x)
//    }
//
//    implicit def pinValueToInt(pinValue: PinValue): Int = pinValue match {
//      case Low => 0
//      case High => 1
//      case Pwm(x) => x
//    }
//
//    implicit def pinPwmValueToInt(pinValue: Pwm): Int = pinValue.value
//
//    implicit def intToPinPwmValue(pinValue: Int): Int = Pwm(pinValue)
//  }
//
//  object PudMode {
//
//    implicit def charToPudMode(pudMode: Int): PudMode = pudMode match {
//      case 0 => PudOff
//      case 1 => PudDown
//      case 2 => PudUp
//      case x => throw new NotImplementedError(s"$x pudMode is not implemented!!")
//    }
//
//    implicit def pudModeToInt(pudMode: PudMode): Int = pudMode match {
//      case PudOff => 0
//      case PudDown => 1
//      case PudUp => 2
//    }
//  }

}
