package com.dcs.charging.session

import com.dcs.charging.tariff.Tariff
import java.math.BigDecimal
import java.math.RoundingMode

object Pricing {

    /** Session fee plus energy times price per kWh, rounded to cents. */
    fun cost(tariff: Tariff, energyKwh: BigDecimal): BigDecimal =
        tariff.sessionFee
            .add(energyKwh.multiply(tariff.pricePerKwh))
            .setScale(2, RoundingMode.HALF_UP)
}
