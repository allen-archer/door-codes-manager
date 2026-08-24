package com.allenarcher.door_codes_manager.automation

import com.allenarcher.door_codes_manager.database.Device
import com.allenarcher.door_codes_manager.database.DeviceRepository
import com.allenarcher.door_codes_manager.database.DoorCode
import com.allenarcher.door_codes_manager.database.DoorCodeRepository
import com.allenarcher.door_codes_manager.state.StateManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AutomatedDoorCodesManagerTest {

    private val stateManager: StateManager = mock()
    private val deviceRepository: DeviceRepository = mock()
    private val doorCodeRepository: DoorCodeRepository = mock()
    private lateinit var manager: AutomatedDoorCodesManager

    private val device = Device(1, "front", "Front Door", 1, 5, 1)

    @BeforeEach
    fun setUp() {
        manager = AutomatedDoorCodesManager(
            objectMapper = mock(),
            stateManager = stateManager,
            deviceRepository = deviceRepository,
            doorCodeRepository = doorCodeRepository,
            address = "",
            port = "",
            topic = "",
            user = "",
            pass = "",
        )
        whenever(deviceRepository.findByName("front")).thenReturn(device)
    }

    @Test
    fun `a duplicate code does not stop later new codes from being added`() {
        whenever(doorCodeRepository.findAllByDevice_Name("front")).thenReturn(
            listOf(DoorCode(1, device, 1, "4785", null, null, null))
        )

        manager.automateDoorCodes(
            listOf(
                AutomatedDoorCode(device = "front", code = "4785"),
                AutomatedDoorCode(device = "front", code = "4423"),
            )
        )

        verify(stateManager, never()).addDoorCode(argThat { code == "4785" })
        verify(stateManager).addDoorCode(argThat { code == "4423" })
    }
}