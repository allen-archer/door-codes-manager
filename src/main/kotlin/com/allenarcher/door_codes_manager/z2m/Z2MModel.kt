package com.allenarcher.door_codes_manager.z2m

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

data class Z2MUser(
    val user: Int? = null,
    @JsonProperty("user_type") val userType: String? = null,
    @JsonProperty("user_enabled") val userEnabled: Boolean? = false,
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("pin_code") val code: String? = null,
    val status: String? = null,
)

data class Z2MResponse(
    val action: String? = null,
    @JsonProperty("action_source_name") val actionSourceName: String? = null,
    @JsonProperty("action_user") val actionUser: Int? = null,
    @JsonProperty("lock_state") val lockState: String? = null,
    val state: String? = null,
    val users: Map<String, Z2MUser> = emptyMap(),
)

data class Z2MRequest(
    @JsonProperty("pin_code") val code: Z2MUser? = null,
)

data class Z2MPinCodeQuery(
    @JsonProperty("pin_code") val code: Z2MPinCodeSlot,
)

data class Z2MPinCodeSlot(
    val user: Int,
)