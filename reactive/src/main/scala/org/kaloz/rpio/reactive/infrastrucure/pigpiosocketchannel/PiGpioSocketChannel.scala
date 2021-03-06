package org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.{ByteBuffer, ByteOrder}
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Function => JFunction}

import com.typesafe.scalalogging.StrictLogging
import io.github.andrebeat.pool.Pool
import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.domain.DomainApi._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.PiGpioSocketChannelRequest
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.PiGpioSocketChannelRequest.domainToInfrastructure
import org.slf4j.MDC

import scala.concurrent.duration._
import scalaz.Scalaz._

object PiGpioSocketChannel extends ProtocolHandlerFactory with StrictLogging {

  implicit def toJavaFunction[A, B](f: Function1[A, B]) = new JFunction[A, B] {
    override def apply(a: A): B = f(a)
  }

  val protocolHandlerMap = new ConcurrentHashMap[Option[Int], PiGpioSocketChannel]()

  def apply(pinNumber: Option[Int] = None): ProtocolHandler = protocolHandlerMap.computeIfAbsent(pinNumber, toJavaFunction(pin => {
    logger.debug(s"Create protocol handler for $pinNumber")
    new PiGpioSocketChannel(pinNumber)
  })
  )
}

class PiGpioSocketChannel(pinNumber: Option[Int]) extends ProtocolHandler with StrictLogging with Configuration {

  val pool = Pool[PiGpioSockerChannelPoolObject](capacity = pigpio.connectionPoolCapacity,
    factory = () => PiGpioSockerChannelPoolObject()(pinNumber),
    reset = _.reset(),
    dispose = _.dispose(),
    healthCheck = pigpio.connectionPoolHealthcheck.fold(_.healthCheck(), _ => true),
    maxIdleTime = if (pigpio.connectionPoolMaxIdleInMinute < 0) Duration.Inf else pigpio.connectionPoolMaxIdleInMinute minute
  )

  pool.fill()

  def request(request: Request): Response = pool.acquire().use(_.sendRequest(request))

  def close(): Unit = pool.drain()
}


case class PiGpioSockerChannelPoolObject()(implicit pinNumber: Option[Int]) extends StrictLogging with Configuration {

  implicit val uuid = UUID.randomUUID().toString
  logger.debug(s"Open channel for $pinNumber:$uuid")

  val socketChannel = SocketChannel.open()
  socketChannel.connect(new InetSocketAddress(pigpio.serverHost, pigpio.serverPort))
  val writeBuffer: ByteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
  logger.debug(s"Channel opened for $pinNumber:$uuid")

  private def sendPiGpioSocketChannelRequest(request: PiGpioSocketChannelRequest): Response = {

    MDC.put("uuid", uuid)
    MDC.put("pin", pinNumber.map(_.toString).getOrElse("N/A"))

    logger.debug(s"Request - $request")
    socketChannel.write(request.fill(writeBuffer))
    val response = request.domainResponse(socketChannel)
    logger.debug(s"Response - $response")

    MDC.clear()
    response
  }

  def sendRequest(request: Request): Response = sendPiGpioSocketChannelRequest(request)

  def reset() = writeBuffer.clear()

  def healthCheck(): Boolean = {
    reset()
    sendPiGpioSocketChannelRequest(VersionRequest()).asInstanceOf[VersionResponse].version > 0
  }

  def dispose() = {
    logger.debug(s"Close channel for $pinNumber:$uuid")
    socketChannel.close()
  }
}
