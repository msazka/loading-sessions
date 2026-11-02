package com.dcs.charging.session

import com.dcs.charging.common.NotFoundException
import com.dcs.charging.tariff.TariffRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class ChargingSessionService(
    private val sessions: ChargingSessionRepository,
    private val tariffs: TariffRepository,
    private val clock: Clock,
) {

    @Transactional
    fun start(request: StartSessionRequest): ChargingSession {
        val tariff = tariffs.findByCode(request.tariffCode)
            ?: throw NotFoundException("Tariff ${request.tariffCode} not found")
        return sessions.save(
            ChargingSession(
                customerId = request.customerId,
                chargePointId = request.chargePointId,
                tariff = tariff,
                startedAt = clock.instant(),
            ),
        )
    }

    @Transactional
    fun stop(id: UUID, request: StopSessionRequest): ChargingSession {
        val session = sessions.findById(id).orElseThrow { NotFoundException("Session $id not found") }
        val energyKwh = requireNotNull(request.energyKwh)

        session.status = SessionStatus.COMPLETED
        session.endedAt = clock.instant()
        session.energyKwh = energyKwh
        session.cost = Pricing.cost(session.tariff, energyKwh)
        // No save() needed: the entity is managed, changes are flushed on commit.
        return session
    }

    @Transactional(readOnly = true)
    fun list(customerId: String?): List<ChargingSession> =
        if (customerId.isNullOrBlank()) {
            sessions.findAllByOrderByStartedAtDesc()
        } else {
            sessions.findByCustomerIdOrderByStartedAtDesc(customerId)
        }
}
