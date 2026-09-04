package com.allenarcher.door_codes_manager.state

import com.allenarcher.door_codes_manager.database.Device
import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.DoorCodeRepository
import com.allenarcher.door_codes_manager.database.DoorCode
import com.allenarcher.door_codes_manager.notifications.Ntfy
import com.allenarcher.door_codes_manager.z2m.Z2MDeviceManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class StateManager(
    private val z2MDeviceManager: Z2MDeviceManager,
    private val doorCodeRepository: DoorCodeRepository,
    private val deviceRepository: DeviceRepository,
    private val ntfy: Ntfy
) {

    private val logger: Logger = LogManager.getLogger()!!
    private val dateFormatter = DateTimeFormatter.ofPattern("M/d/yy H:mm")

    fun addDoorCode(doorCode: DoorCode): Boolean {
        doorCode.expirationDate?.let {
            if (it < LocalDateTime.now()) {
                logger.warn("Attempting to add an expired code: $doorCode")
                return false
            }
        }
        doorCode.startDate?.let {
            if (it < LocalDateTime.now()) {
                logger.warn("Adding a door code with an expired start date. Deleting the start date and adding the code as normal: $doorCode")
                doorCode.startDate = null
            } else {
                logger.info("Saving door code with a start date in the future to the database: $doorCode")
                doorCodeRepository.save(doorCode)
                return true
            }
        }
        val response = z2MDeviceManager.setCode(doorCode.device.name, doorCode.slot, doorCode.code)
        if (response != null) {
            doorCodeRepository.save(doorCode)
            logger.info("Slot {}, code {}, added to device {}", doorCode.slot, doorCode.code, doorCode.device.name)
        } else {
            logger.error("Error setting slot {}, code {} on device {}", doorCode.slot, doorCode.code, doorCode.device.name)
        }
        return response != null
    }

    fun updateDoorCode(newDoorCode: DoorCode, existingDoorCode: DoorCode): Boolean {
        val codeChanged = newDoorCode.code != existingDoorCode.code
        if (codeChanged) {
            val response = z2MDeviceManager.setCode(newDoorCode.device.name, newDoorCode.slot, newDoorCode.code)
            if (response == null) {
                logger.error("Error setting slot {}, code {} on device {}", newDoorCode.slot, newDoorCode.code, newDoorCode.device.name)
                return false
            }
        }
        doorCodeRepository.save(newDoorCode)
        logger.info("Slot {}, code {}, updated on device {}", newDoorCode.slot, newDoorCode.code, newDoorCode.device.name)
        return true
    }

    fun deleteDoorCode(doorCode: DoorCode): Boolean {
        val response = z2MDeviceManager.deleteCode(doorCode.device.name, doorCode.slot)
        if (response != null) {
            doorCodeRepository.delete(doorCode)
            logger.info("Slot {}, code {}, deleted from device {}", doorCode.slot, doorCode.code, doorCode.device.name)
        } else {
            logger.error("Error deleting slot {}, code {}, from device {}", doorCode.slot, doorCode.code, doorCode.device.name)
        }
        return response != null
    }

    fun syncDoorCodes(
        device: Device,
        start: Int,
        end: Int,
        onSlotSynced: (Int) -> Unit = {},
        onMessage: (String) -> Unit = ::println,
    ): Boolean {
        val deviceName = device.name
        var low = start
        var high = end
        if (low < device.slotMin) {
            low = device.slotMin
        }
        if (low > device.slotMax) {
            return false
        }
        if (high < low) {
            return false
        }
        if (high > device.slotMax) {
            high = device.slotMax
        }
        var updates = false
        for (slot in low..high) {
            val doorCodeInDatabase = doorCodeRepository.findByDevice_NameAndSlot(deviceName, slot)
            val response = z2MDeviceManager.getCode(deviceName, slot)
            if (response != null) {
                val user = response.users[slot.toString()]
                if (user != null) {
                    val status = user.status
                    val pinCode = user.code
                    if (status == "available") {
                        if (doorCodeInDatabase != null) {
                            if (doorCodeInDatabase.startDate != null) {
                                val message =
                                    "Slot $slot on device $deviceName is available on the device, but set in the database with a startDate, ignoring."
                                logger.info(message)
                                onMessage(message)
                            } else {
                                updates = true
                                val message =
                                    "Slot $slot on device $deviceName is available on the device, but set in the database. Deleting from the database."
                                logger.info(message)
                                onMessage(message)
                                doorCodeRepository.delete(doorCodeInDatabase)
                            }
                        }
                    } else if (status == "disabled") {
                        if (doorCodeInDatabase != null) {
                            updates = true
                            val message =
                                "Slot $slot on device $deviceName is disabled on the device, but set in the database. Deleting from the device and the database."
                            logger.info(message)
                            onMessage(message)
                            doorCodeRepository.delete(doorCodeInDatabase)
                        } else {
                            updates = true
                            val message =
                                "Slot $slot on device $deviceName is disabled on the device. Deleting from the device."
                            logger.info(message)
                            onMessage(message)
                        }
                        val response = z2MDeviceManager.deleteCode(deviceName, slot)
                        if (response == null) {
                            val message = "Error deleting $slot on device $deviceName"
                            logger.info(message)
                            onMessage(message)
                        }
                    } else if (status == "enabled") {
                        if (doorCodeInDatabase == null) {
                            updates = true
                            val message = "Adding slot $slot on device $deviceName."
                            logger.info(message)
                            onMessage(message)
                            doorCodeRepository.save(
                                DoorCode(
                                    null,
                                    device,
                                    slot,
                                    pinCode!!,
                                    "Existed on device",
                                    null,
                                    null
                                )
                            )
                        } else if (pinCode!! == doorCodeInDatabase.code) {
                            updates = true
                            val message =
                                "Slot $slot on device $deviceName exists on the device and database and they have the same pin $pinCode"
                            logger.info(message)
                            onMessage(message)
                        } else {
                            updates = true
                            val message =
                                "Slot $slot on device $deviceName exists on the device and database but they have different pin codes. Updating to what was on the device."
                            logger.info(message)
                            onMessage(message)
                            doorCodeInDatabase.code = pinCode
                            doorCodeRepository.save(doorCodeInDatabase)
                        }
                    }
                }
            }
            onSlotSynced(slot)
        }
        return updates
    }

    fun deleteDevice(device: Device) {
        doorCodeRepository.deleteAllByDevice_Name(device.name)
        deviceRepository.delete(device)
    }

    @Scheduled(fixedRateString = $$"${code-start-and-expiration-check-timer}")
    fun doorCodeScheduledDateCheck() {
        val now = LocalDateTime.now()
        doorCodeRepository.findAllByStartDateBefore(now).forEach {
            it.startDate = null
            val added = addDoorCode(it)
            if (added) {
                val expirationMessage = if (it.expirationDate != null) ", expires at ${dateFormatter.format(it.expirationDate)}" else ""
                ntfy.sendNotification("Door code added", "Door code added for ${it.description} on device ${it.device.name}$expirationMessage", "3", "white_check_mark")
            } else {
                ntfy.sendNotification("MQTT error", "Error setting code for ${it.description} on device ${it.device.name}", "4", "no_entry")
            }
        }
        doorCodeRepository.findAllByExpirationDateBefore(now).forEach {
            val deleted = deleteDoorCode(it)
            if (deleted) {
                ntfy.sendNotification("Door code deleted", "Door code deleted for ${it.description} on device ${it.device.name}", "3", "x")
            } else {
                ntfy.sendNotification("MQTT error", "Error deleting code for ${it.description} on device ${it.device.name}", "4", "no_entry")
            }
        }
    }
}