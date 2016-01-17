package org.kaloz.rpio.reactive.config

import com.typesafe.config.ConfigFactory

trait Configuration {

  private lazy val config = ConfigFactory.load("rpi-reactive")

  object pigpio {
    val pigpioConf = config.getConfig("pigpio")

    val serverHost = pigpioConf.getString("server.host")
    val serverPort = pigpioConf.getInt("server.port")

    val connectionPoolCapacity = pigpioConf.getInt("connection-pool.capacity")
    val connectionPoolMaxIdleInMinute = pigpioConf.getInt("connection-pool.max-ide-minute")
    val connectionPoolHealthcheck = pigpioConf.getBoolean("connection-pool.healthcheck")
  }


  object observablePin {
    val observablePinConf = config.getConfig("observable-pin")

    val refreshInterval = observablePinConf.getInt("refresh-interval")
  }

}
