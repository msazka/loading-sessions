package com.dcs.charging.session

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChargingSessionRepository : JpaRepository<ChargingSession, UUID> {

    fun findAllByOrderByStartedAtDesc(): List<ChargingSession>

    fun findByCustomerIdOrderByStartedAtDesc(customerId: String): List<ChargingSession>
}
