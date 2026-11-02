package com.dcs.charging.session

import com.dcs.charging.tariff.Tariff
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class PricingTest {

    private val tariff = Tariff("TEST", "Test", BigDecimal("0.5900"), BigDecimal("0.35"))

    @Test
    fun `adds session fee to energy cost`() {
        assertEquals(BigDecimal("12.15"), Pricing.cost(tariff, BigDecimal("20.000")))
    }

    @Test
    fun `rounds half up to cents`() {
        // 0.35 + 12.345 * 0.59 = 7.63355
        assertEquals(BigDecimal("7.63"), Pricing.cost(tariff, BigDecimal("12.345")))
    }

    @Test
    fun `zero energy costs only the session fee`() {
        assertEquals(BigDecimal("0.35"), Pricing.cost(tariff, BigDecimal.ZERO))
    }
}
