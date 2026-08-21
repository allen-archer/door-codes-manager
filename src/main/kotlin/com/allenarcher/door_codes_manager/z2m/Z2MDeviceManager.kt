package com.allenarcher.door_codes_manager.z2m

import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.Device
import jakarta.annotation.PostConstruct
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class Z2MDeviceManager(
    private val objectMapper: ObjectMapper,
    private val deviceRepository: DeviceRepository,
    @Value($$"${zigbee2Mqtt.mqtt.address}") private val address: String,
    @Value($$"${zigbee2Mqtt.mqtt.port:1883}") private val port: String,
    @Value($$"${zigbee2Mqtt.mqtt.topic:zigbee2mqtt}") private val topic: String,
    @Value($$"${zigbee2Mqtt.mqtt.timeout:15}") private val timeout: Long,
    @Value($$"${zigbee2Mqtt.mqtt.username:}") private val user: String,
    @Value($$"${zigbee2Mqtt.mqtt.password:}") private val pass: String,
) {
    private lateinit var client: MqttClient
    private val logger: Logger = LogManager.getLogger()

    private val pendingActions = ConcurrentHashMap<Pair<String, String?>, CompletableFuture<Z2MResponse>>()
    private val deviceLocks = ConcurrentHashMap<String, Any>()

    @PostConstruct
    fun connect() {
        if (address.isEmpty() || port.isEmpty() || topic.isEmpty()) {
            logger.warn("Zigbee2MQTT config missing, not connecting: address={}, port={}, topic={}", address, port, topic)
            return
        }
        client = MqttClient("$address:$port", MqttClient.generateClientId(), MemoryPersistence())
        client.connect(MqttConnectOptions().apply {
            isAutomaticReconnect = true
            if (user.isNotBlank()) {
                userName = user
                password = pass.toCharArray()
            }
        })
        client.subscribe("$topic/#") { receivedTopic, message ->
            val friendlyName = receivedTopic.removePrefix("$topic/")
            val device = deviceRepository.findByFriendlyName(friendlyName) ?: return@subscribe
            try {
                val response = objectMapper.readValue(message.payload, Z2MResponse::class.java)
                handle(device, response)
            } catch (e: Exception) {
                println("Failed to handle message on $receivedTopic: $e")
            }
        }
    }

    private fun handle(device: Device, response: Z2MResponse) {
        pendingActions.remove(device.name to response.action)?.complete(response)
    }

    fun setCode(deviceName: String, slot: Int, code: String, timeoutSeconds: Long = timeout): Z2MResponse? =
        awaitConfirmation(deviceName, "pin_code_added", timeoutSeconds) {
            publish(deviceName, "set", Z2MRequest(Z2MUser(slot, "unrestricted", true, code, null)))
        }

    private fun awaitConfirmation(deviceName: String, actionType: String?, timeoutSeconds: Long, action: () -> Unit): Z2MResponse? =
        synchronized(deviceLocks.computeIfAbsent(deviceName) { Any() }) {
            val key = deviceName to actionType
            val future = CompletableFuture<Z2MResponse>()
            pendingActions[key] = future
            action()
            try {
                future.get(timeoutSeconds, TimeUnit.SECONDS)
            } catch (_: TimeoutException) {
                null
            } finally {
                pendingActions.remove(key)
            }
        }

    fun getCode(deviceName: String, slot: Int, timeoutSeconds: Long = timeout): Z2MResponse? =
        awaitConfirmation(deviceName, null, timeoutSeconds) {
            publish(deviceName, "get", Z2MPinCodeQuery(Z2MPinCodeSlot(slot)))
        }

    fun deleteCode(deviceName: String, slot: Int, timeoutSeconds: Long = timeout): Z2MResponse? =
        awaitConfirmation(deviceName, "pin_code_deleted", timeoutSeconds) {
            publish(deviceName, "set", Z2MRequest(Z2MUser(slot, null, true, null, null)))
        }

    private fun publish(deviceName: String, action: String, payload: Any) {
        val device = deviceRepository.findByName(deviceName)
        if (device == null) {
            logger.error("No device named $deviceName is configured.")
            return
        }
        client.publish(
            "$topic/${device.friendlyName}/$action",
            MqttMessage(objectMapper.writeValueAsBytes(payload))
        )
    }
}