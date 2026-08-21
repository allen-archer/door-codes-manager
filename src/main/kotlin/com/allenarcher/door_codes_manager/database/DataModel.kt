package com.allenarcher.door_codes_manager.database

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["device_id", "code"])])
class DoorCode(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne
    var device: Device,
    var slot: Int,
    var code: String,
    var description: String? = null,
    var startDate: LocalDateTime?,
    var expirationDate: LocalDateTime?,
) {
    override fun toString() =
        "DoorCode(device=${device.id}, slot=$slot, code=$code, description=$description, expirationDate=$expirationDate)"
}

@Entity
class Device(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(unique = true)
    var name: String,
    var friendlyName: String,
    var slotMin: Int,
    var slotMax: Int,
    var automatedSlotStart: Int,
) {
    override fun toString() =
        "Device(name=$name, friendlyName=$friendlyName, slotMin=$slotMin, slotMax=$slotMax, automatedSlotStart=$automatedSlotStart)"
}
