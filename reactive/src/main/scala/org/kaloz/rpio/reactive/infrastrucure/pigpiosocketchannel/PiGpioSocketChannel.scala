package org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel

import java.net.InetSocketAddress
import java.nio.{ByteOrder, ByteBuffer}
import java.nio.channels.SocketChannel
import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import io.github.andrebeat.pool.Pool
import org.kaloz.rpio.reactive.domain.DomainApi.{ProtocolHandlerFactory, ProtocolHandler, Request, Response}
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi.PiGpioSocketChannelRequest.domainToInfrastructure
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object PiGpioSocketChannel extends ProtocolHandlerFactory {
  def apply(pin: Option[Int] = None): ProtocolHandler = new PiGpioSocketChannel(pin)
}

class PiGpioSocketChannel(pin: Option[Int] = None) extends ProtocolHandler with StrictLogging {

  val pool = Pool[PiGpioSockerChannelPoolObject](capacity = 3,
    factory = () => PiGpioSockerChannelPoolObject()(pin),
    reset = socketChannelPoolObject => socketChannelPoolObject.clear(),
    dispose = socketChannelPoolObject => socketChannelPoolObject.close()
  )

  pool.fill()

  def request(request: Request): Future[Response] = pool.acquire().use(_.sendRequest(request))

  def close(): Future[Unit] = Future(pool.drain())
}


case class PiGpioSockerChannelPoolObject()(implicit pin: Option[Int]) extends StrictLogging {

  implicit val uuid = UUID.randomUUID().toString
  logger.info(s"Open channel for $pin:$uuid")

  val socketChannel = SocketChannel.open()
  socketChannel.connect(new InetSocketAddress("192.168.1.239", 8888))
  val writeBuffer: ByteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
  logger.info(s"Channel opened for $pin:$uuid")

  def sendRequest(request: Request): Future[Response] = Future {
    logger.info(s"Request - $pin:$uuid:$request")

    val piGpioSocketChannelRequest: PiGpioSocketChannelRequest = request
    socketChannel.write(piGpioSocketChannelRequest.fill(writeBuffer))
    val responseHandler = piGpioSocketChannelRequest.responseHandler
    socketChannel.read(responseHandler.readBuffer)
    val response = responseHandler.response

    logger.info(s"Response - $pin:$uuid:$request")

    response
  }

  def clear() = writeBuffer.clear()

  def close() = {
    logger.info(s"Close channel for $pin:$uuid")
    socketChannel.close()
  }
}
