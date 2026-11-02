package com.dcs.charging.tariff

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal

/** Seeds a few tariffs so the app is usable on an empty database. */
@Component
class TariffSeeder(private val tariffs: TariffRepository) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (tariffs.count() > 0) return
        tariffs.saveAll(
            listOf(
                Tariff("AC_STANDARD", "AC Standard", BigDecimal("0.5900"), BigDecimal("0.00")),
                Tariff("DC_FAST", "DC Fast", BigDecimal("0.7900"), BigDecimal("0.00")),
                Tariff("SUBSCRIPTION_M", "Subscription M", BigDecimal("0.4500"), BigDecimal("0.35")),
            ),
        )
    }
}
