package com.dcs.charging.session

import com.dcs.charging.tariff.Tariff
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class SessionStatus { ACTIVE, COMPLETED }

@Entity
@Table(name = "charging_session")
class ChargingSession(
    @Column(nullable = false)
    var customerId: String,

    @Column(nullable = false)
    var chargePointId: String,

    @ManyToOne(optional = false)
    @JoinColumn(name = "tariff_id")
    var tariff: Tariff,

    @Column(nullable = false)
    var startedAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SessionStatus = SessionStatus.ACTIVE

    var endedAt: Instant? = null

    @Column(precision = 10, scale = 3)
    var energyKwh: BigDecimal? = null

    /** Total cost in EUR, gross. Set when the session is stopped. */
    @Column(precision = 10, scale = 2)
    var cost: BigDecimal? = null
}
