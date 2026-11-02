package com.dcs.charging.session

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class StartSessionRequest(
    @field:NotBlank(message = "customerId must not be blank")
    val customerId: String,

    @field:NotBlank(message = "chargePointId must not be blank")
    val chargePointId: String,

    @field:NotBlank(message = "tariffCode must not be blank")
    val tariffCode: String,
)

data class StopSessionRequest(
    @field:NotNull(message = "energyKwh is required")
    @field:DecimalMin(value = "0.0", message = "energyKwh must not be negative")
    @field:DecimalMax(value = "500.0", message = "energyKwh must be at most 500")
    val energyKwh: BigDecimal?,
)

data class SessionResponse(
    val id: UUID,
    val customerId: String,
    val chargePointId: String,
    val tariffCode: String,
    val status: SessionStatus,
    val startedAt: Instant,
    val endedAt: Instant?,
    val energyKwh: BigDecimal?,
    val cost: BigDecimal?,
) {
    companion object {
        fun from(session: ChargingSession) = SessionResponse(
            id = session.id!!,
            customerId = session.customerId,
            chargePointId = session.chargePointId,
            tariffCode = session.tariff.code,
            status = session.status,
            startedAt = session.startedAt,
            endedAt = session.endedAt,
            energyKwh = session.energyKwh,
            cost = session.cost,
        )
    }
}
