package org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.{ByteBuffer, ByteOrder}
import java.util.UUID

import com.typesafe.scalalogging.StrictLogging
import io.github.andrebeat.pool.Pool
import org.kaloz.rpio.reactive.config.Configuration
import org.kaloz.rpio.reactive.domain.api._
import org.kaloz.rpio.reactive.infrastrucure.pigpiosocketchannel.InfrastructureApi._
import org.slf4j.MDC

import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz._

object PiGpioSocketChannel extends SendReceiveHandler with StrictLogging with Configuration {

  val pool = Pool[PiGpioSocketChannelPoolObject](capacity = PigpioServerConf.connectionPoolCapacity,
    factory = () => PiGpioSocketChannelPoolObject(PigpioServerConf.serverHost, PigpioServerConf.serverPort),
    reset = _.reset(),
    dispose = _.dispose(),
    healthCheck = PigpioServerConf.connectionPoolHealthcheck.fold(_.healthCheck(), _ => true),
    maxIdleTime = if (PigpioServerConf.connectionPoolMaxIdleInMinute < 0) Duration.Inf else PigpioServerConf.connectionPoolMaxIdleInMinute minute
  )

  pool.fill()

  def sendReceive[A <: DomainResponse](request: DomainRequest): Valid[A] = pool.acquire().use(_.sendReceive[A](request))

  def close(): Unit = pool.drain()

  case class PiGpioSocketChannelPoolObject(serverHost: String, serverPort: Int) extends StrictLogging {

    val channelId = UUID.randomUUID().toString
    logger.debug(s"Opening channel $channelId")
    val socketChannel = SocketChannel.open()
    socketChannel.connect(new InetSocketAddress(serverHost, serverPort))
    val writeBuffer: ByteBuffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
    logger.debug(s"Channel opened for $channelId")

    def sendReceive[B <: DomainResponse](request: PiGpioSocketChannelRequest): Valid[B] = \/.fromTryCatchThrowable[B, Throwable] {
      MDC.put("channelId", channelId)
      logger.debug(s"Request - $request")

      socketChannel.write(request.fill(writeBuffer))
      val response = request.domainResponse(socketChannel)
      logger.debug(s"Response - $response")

      MDC.clear()
      response.asInstanceOf[B]
    }

    def reset() = writeBuffer.clear()

    def healthCheck(): Boolean = {
      reset()
      sendReceive[VersionResponse](VersionRequest()).fold(_ => false, _.version > 0)
    }

    def dispose() = socketChannel.close()
  }

}


