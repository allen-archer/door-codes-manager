package com.allenarcher.door_codes_manager.automation

import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.DoorCodeRepository
import com.allenarcher.door_codes_manager.database.DoorCode
import com.allenarcher.door_codes_manager.state.StateManager
import jakarta.annotation.PostConstruct
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

@Component
class AutomatedDoorCodesManager(
    private val objectMapper: ObjectMapper,
    private val stateManager: StateManager,
    private val deviceRepository: DeviceRepository,
    private val doorCodeRepository: DoorCodeRepository,
    @Value($$"${automation.mqtt.address}") private val address: String,
    @Value($$"${automation.mqtt.port:1883}") private val port: String,
    @Value($$"${automation.mqtt.topic:}") private val topic: String,
    @Value($$"${automation.mqtt.username:}") private val user: String,
    @Value($$"${automation.mqtt.password:}") private val pass: String,
) {
    private lateinit var client: MqttClient
    private val logger: Logger = LogManager.getLogger()

    @PostConstruct
    fun connect() {
        if (address.isEmpty() || port.isEmpty() || topic.isEmpty()) {
            logger.warn("Automated door codes MQTT config missing, not automating door codes: address={}, port={}, topic={}", address, port, topic)
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
        client.subscribe(topic) { _, message ->
            try {
                val doorCodesToAutomateList = objectMapper.readValue(message.payload, object : TypeReference<List<AutomatedDoorCode>>() {})
                if (doorCodesToAutomateList == null || doorCodesToAutomateList.isEmpty()) {
                    logger.warn("Message received on automated topic, but data was empty.")
                    return@subscribe
                }
                automateDoorCodes(doorCodesToAutomateList)
            } catch (e: Exception) {
                println("Failed to handle message on $topic: $e")
            }
        }
    }

    internal fun automateDoorCodes(doorCodesToAutomateList: List<AutomatedDoorCode>) {
        loop@ for (entry in doorCodesToAutomateList.groupBy { it.device }) {
            val deviceName = entry.key ?: continue@loop
            val deviceRow = deviceRepository.findByName(deviceName) ?: continue@loop
            val allDoorCodes = doorCodeRepository.findAllByDevice_Name(deviceName)
            val usedCodes = allDoorCodes.map { it.code }.toSet()
            val usedSlots = allDoorCodes.map { it.slot }.toSet()
            val availableSlots = (deviceRow.automatedSlotStart..deviceRow.slotMax)
                .filter { !usedSlots.contains(it) }
                .toMutableList()
            entry.value.forEach {
                val code = it.code
                if (usedCodes.contains(code)) {
                    logger.info("Code $code already exists on device $deviceName, skipping.")
                    return@forEach
                }
                if (availableSlots.isEmpty()) {
                    logger.error("No available automated slots left for $deviceName.")
                    continue@loop
                }
                val slot = availableSlots.removeFirst()
                val doorCode = DoorCode(null, deviceRow, slot, code!!, it.description, it.startDate, it.expirationDate)
                stateManager.addDoorCode(doorCode)
            }
        }
    }
}