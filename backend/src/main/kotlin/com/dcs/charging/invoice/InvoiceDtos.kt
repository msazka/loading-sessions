package com.dcs.charging.invoice

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class InvoiceLine(
    val sessionId: UUID,
    val endedAt: Instant,
    val tariffCode: String,
    val energyKwh: BigDecimal,
    val cost: BigDecimal,
)

data class InvoiceResponse(
    val customerId: String,
    val month: String,
    val lines: List<InvoiceLine>,
    val totalEnergyKwh: BigDecimal,
    val totalGross: BigDecimal,
    val vatRate: BigDecimal,
    val totalNet: BigDecimal,
    val totalVat: BigDecimal,
)
