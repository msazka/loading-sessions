package com.dcs.charging.invoice

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class VatTest {

    @Test
    fun `net of the ticket example rounds up to cents`() {
        // 11.80 / 1.19 = 9.9159...
        assertEquals(BigDecimal("9.92"), Vat.netOf(BigDecimal("11.80")))
    }

    @Test
    fun `net rounds down below the halfway point`() {
        // 1.00 / 1.19 = 0.8403...
        assertEquals(BigDecimal("0.84"), Vat.netOf(BigDecimal("1.00")))
    }

    @Test
    fun `zero gross has zero net with two decimals`() {
        assertEquals(BigDecimal("0.00"), Vat.netOf(BigDecimal("0.00")))
    }
}
