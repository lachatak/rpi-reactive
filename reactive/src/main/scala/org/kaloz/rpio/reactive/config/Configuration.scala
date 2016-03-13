package org.kaloz.rpio.reactive.config

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.FiniteDuration

trait Configuration {

  private lazy val config = ConfigFactory.load("rpi-reactive")

  object PigpioServerConf {
    private lazy val pigpioServerConf = config.getConfig("pigpio.server")

    val serverHost = pigpioServerConf.getString("host")
    val serverPort = pigpioServerConf.getInt("port")

    val connectionPoolCapacity = pigpioServerConf.getInt("connection-pool.capacity")
    val connectionPoolMaxIdleInMinute = pigpioServerConf.getInt("connection-pool.max-ide-minute")
    val connectionPoolHealthcheck = pigpioServerConf.getBoolean("connection-pool.healthcheck")
  }


  object ObservablePinConf {
    private lazy val observablePinConf = config.getConfig("observable-pin")

    val refreshInterval = FiniteDuration(observablePinConf.getInt("refresh-interval"), TimeUnit.MILLISECONDS)
  }

}
