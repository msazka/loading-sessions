package com.dcs.charging.tariff

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "tariff")
class Tariff(
    @Column(nullable = false, unique = true)
    var code: String,

    @Column(nullable = false)
    var name: String,

    /** Price per kWh in EUR, gross. */
    @Column(nullable = false, precision = 8, scale = 4)
    var pricePerKwh: BigDecimal,

    /** Flat fee per charging session in EUR, gross. */
    @Column(nullable = false, precision = 8, scale = 2)
    var sessionFee: BigDecimal,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
