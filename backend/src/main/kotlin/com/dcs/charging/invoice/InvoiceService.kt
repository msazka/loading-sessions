package com.dcs.charging.invoice

import com.dcs.charging.session.ChargingSessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Invoice months are calendar months in German time, whatever the server's time zone is. */
private val INVOICE_ZONE: ZoneId = ZoneId.of("Europe/Berlin")

@Service
class InvoiceService(private val sessions: ChargingSessionRepository) {

    @Transactional(readOnly = true)
    fun invoice(customerId: String, month: YearMonth): InvoiceResponse {
        val lines = sessions
            .findCompletedEndedBetween(customerId, startOf(month), startOf(month.plusMonths(1)))
            .map {
                InvoiceLine(
                    sessionId = it.id!!,
                    endedAt = it.endedAt!!,
                    tariffCode = it.tariff.code,
                    energyKwh = it.energyKwh!!,
                    cost = it.cost!!,
                )
            }

        // VAT is worked out once on the total: rounding per line can give a different result.
        val totalGross = lines.fold(BigDecimal("0.00")) { sum, line -> sum.add(line.cost) }
        val totalNet = Vat.netOf(totalGross)

        return InvoiceResponse(
            customerId = customerId,
            month = month.toString(),
            lines = lines,
            totalEnergyKwh = lines.fold(BigDecimal("0.000")) { sum, line -> sum.add(line.energyKwh) },
            totalGross = totalGross,
            vatRate = Vat.RATE,
            totalNet = totalNet,
            totalVat = totalGross.subtract(totalNet),
        )
    }

    private fun startOf(month: YearMonth): Instant =
        month.atDay(1).atStartOfDay(INVOICE_ZONE).toInstant()
}
