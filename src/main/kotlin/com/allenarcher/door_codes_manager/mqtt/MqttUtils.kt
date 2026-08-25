package com.allenarcher.door_codes_manager.mqtt

import org.apache.logging.log4j.Logger
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

fun resubscribingCallback(logger: Logger, name: String, subscribe: () -> Unit): MqttCallbackExtended =
    object : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String) {
            logger.info("{} connected (reconnect={}), subscribing", name, reconnect)
            subscribe()
        }
        override fun connectionLost(cause: Throwable) = logger.warn("{} connection lost", name, cause)
        override fun messageArrived(topic: String, message: MqttMessage) {}
        override fun deliveryComplete(token: IMqttDeliveryToken) {}
    }

fun mqttConnectionOptions(user: String?, pass: String?): MqttConnectOptions = MqttConnectOptions().apply {
    isAutomaticReconnect = true
    if (user?.isNotBlank() == true && pass?.isNotBlank() == true) {
        userName = user
        password = pass.toCharArray()
    }
}