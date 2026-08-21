package com.allenarcher.door_codes_manager.database

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface DoorCodeRepository : JpaRepository<DoorCode, Long> {
    fun findAllByDevice_Name(deviceName: String): List<DoorCode>
    fun findByDevice_NameAndSlot(deviceName: String, slot: Int): DoorCode?
    fun findByDevice_NameAndCode(deviceName: String, code: String): DoorCode?
    fun findAllByStartDateBefore(now: LocalDateTime): List<DoorCode>
    fun findAllByExpirationDateBefore(now: LocalDateTime): List<DoorCode>
    fun findAllByOrderBySlot(): List<DoorCode>
    fun deleteAllByDevice_Name(deviceName: String): List<DoorCode>
}

interface DeviceRepository : JpaRepository<Device, Long> {
    fun findByName(name: String): Device?
    fun findByFriendlyName(friendlyName: String): Device?
}
