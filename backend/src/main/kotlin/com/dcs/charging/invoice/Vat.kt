package com.dcs.charging.invoice

import java.math.BigDecimal
import java.math.RoundingMode

object Vat {

    val RATE: BigDecimal = BigDecimal("0.19")

    /** Net amount contained in a gross amount, rounded half-up to cents. */
    fun netOf(gross: BigDecimal): BigDecimal =
        gross.divide(BigDecimal.ONE.add(RATE), 2, RoundingMode.HALF_UP)
}
