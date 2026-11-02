package com.dcs.charging.session

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface ChargingSessionRepository : JpaRepository<ChargingSession, UUID> {

    fun findAllByOrderByStartedAtDesc(): List<ChargingSession>

    fun findByCustomerIdOrderByStartedAtDesc(customerId: String): List<ChargingSession>

    /** Completed sessions of a customer that ended in [from, until). */
    @Query(
        """
        select s from ChargingSession s
        join fetch s.tariff
        where s.customerId = :customerId
          and s.status = com.dcs.charging.session.SessionStatus.COMPLETED
          and s.endedAt >= :from
          and s.endedAt < :until
        order by s.endedAt, s.id
        """,
    )
    fun findCompletedEndedBetween(
        @Param("customerId") customerId: String,
        @Param("from") from: Instant,
        @Param("until") until: Instant,
    ): List<ChargingSession>
}
