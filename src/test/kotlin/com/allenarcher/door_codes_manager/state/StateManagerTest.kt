package com.allenarcher.door_codes_manager.state

import com.allenarcher.door_codes_manager.database.Device
import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.DoorCode
import com.allenarcher.door_codes_manager.database.DoorCodeRepository
import com.allenarcher.door_codes_manager.z2m.Z2MDeviceManager
import com.allenarcher.door_codes_manager.z2m.Z2MResponse
import com.allenarcher.door_codes_manager.z2m.Z2MUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class StateManagerTest {

    private val z2MDeviceManager: Z2MDeviceManager = mock()
    private val doorCodeRepository: DoorCodeRepository = mock()
    private val deviceRepository: DeviceRepository = mock()
    private lateinit var stateManager: StateManager

    private val device = Device(1, "front-door", "Front Door", 1, 5, 3)

    @BeforeEach
    fun setUp() {
        stateManager = StateManager(z2MDeviceManager, doorCodeRepository, deviceRepository)
    }

    @Test
    fun `addDoorCode saves when device confirms`() {
        val doorCode = DoorCode(null, device, 1, "1234", null, null, null)
        whenever(z2MDeviceManager.setCode("front-door", 1, "1234")).thenReturn(Z2MResponse())

        val result = stateManager.addDoorCode(doorCode)

        assertTrue(result)
        verify(doorCodeRepository).save(doorCode)
    }

    @Test
    fun `addDoorCode does not save when device does not confirm`() {
        val doorCode = DoorCode(null, device, 1, "1234", null, null, null)
        whenever(z2MDeviceManager.setCode("front-door", 1, "1234")).thenReturn(null)

        val result = stateManager.addDoorCode(doorCode)

        assertFalse(result)
        verify(doorCodeRepository, never()).save(any())
    }

    @Test
    fun `deleteDoorCode deletes when device confirms`() {
        val doorCode = DoorCode(2, device, 1, "1234", null, null, null)
        whenever(z2MDeviceManager.deleteCode("front-door", 1)).thenReturn(Z2MResponse())

        val result = stateManager.deleteDoorCode(doorCode)

        assertTrue(result)
        verify(doorCodeRepository).delete(doorCode)
    }

    @Test
    fun `syncDoorCodes clears database entry for slot that is available on device`() {
        val existing = DoorCode(3, device, 2, "5555", null, null, null)
        whenever(doorCodeRepository.findByDevice_NameAndSlot("front-door", 2)).thenReturn(existing)
        whenever(z2MDeviceManager.getCode("front-door", 2))
            .thenReturn(Z2MResponse(users = mapOf("2" to Z2MUser(status = "available"))))

        val updates = stateManager.syncDoorCodes(device, 2, 2)

        assertTrue(updates)
        verify(doorCodeRepository).delete(existing)
    }

    @Test
    fun `syncDoorCodes adds database entry for slot enabled on device but missing locally`() {
        whenever(doorCodeRepository.findByDevice_NameAndSlot("front-door", 2)).thenReturn(null)
        whenever(z2MDeviceManager.getCode("front-door", 2))
            .thenReturn(Z2MResponse(users = mapOf("2" to Z2MUser(status = "enabled", code = "9999"))))
        whenever(doorCodeRepository.save(any<DoorCode>())).thenAnswer { it.arguments[0] }

        val updates = stateManager.syncDoorCodes(device, 2, 2)

        assertTrue(updates)
        verify(doorCodeRepository).save(any())
    }

    @Test
    fun `syncDoorCodes updates code when device and database disagree`() {
        val existing = DoorCode(3, device, 2, "1111", null, null, null)
        whenever(doorCodeRepository.findByDevice_NameAndSlot("front-door", 2)).thenReturn(existing)
        whenever(z2MDeviceManager.getCode("front-door", 2))
            .thenReturn(Z2MResponse(users = mapOf("2" to Z2MUser(status = "enabled", code = "9999"))))
        whenever(doorCodeRepository.save(any<DoorCode>())).thenAnswer { it.arguments[0] }

        val updates = stateManager.syncDoorCodes(device, 2, 2)

        assertTrue(updates)
        assertEquals("9999", existing.code)
        verify(doorCodeRepository).save(existing)
    }

    @Test
    fun `syncDoorCodes clamps range to device slot bounds`() {
        whenever(doorCodeRepository.findByDevice_NameAndSlot(eq("front-door"), any())).thenReturn(null)
        whenever(z2MDeviceManager.getCode(eq("front-door"), any(), any())).thenReturn(null)

        stateManager.syncDoorCodes(device, 0, 10)

        verify(z2MDeviceManager).getCode(eq("front-door"), eq(1), any())
        verify(z2MDeviceManager).getCode(eq("front-door"), eq(5), any())
        verify(z2MDeviceManager, never()).getCode(eq("front-door"), eq(6), any())
    }

    @Test
    fun `syncDoorCodes returns false when start is after device slot max`() {
        val result = stateManager.syncDoorCodes(device, 10, 12)

        assertFalse(result)
        verify(z2MDeviceManager, never()).getCode(any(), any(), any())
    }
}