package com.allenarcher.door_codes_manager.automation

import java.time.LocalDateTime

data class AutomatedDoorCode(
    val device: String? = null,
    val code: String? = null,
    val description: String? = null,
    val startDate: LocalDateTime? = null,
    val expirationDate: LocalDateTime? = null,
)