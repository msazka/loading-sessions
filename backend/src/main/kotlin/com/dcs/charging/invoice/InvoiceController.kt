package com.dcs.charging.invoice

import com.dcs.charging.common.BadRequestException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.DateTimeException
import java.time.YearMonth

// YearMonth.parse alone also accepts values such as "+12026-11".
private val MONTH_FORMAT = Regex("""\d{4}-\d{2}""")

@RestController
class InvoiceController(private val service: InvoiceService) {

    @GetMapping("/api/customers/{customerId}/invoices/{month}")
    fun invoice(@PathVariable customerId: String, @PathVariable month: String): InvoiceResponse =
        service.invoice(customerId, parseMonth(month))

    private fun parseMonth(value: String): YearMonth {
        if (!MONTH_FORMAT.matches(value)) throw invalidMonth(value)
        return try {
            YearMonth.parse(value)
        } catch (_: DateTimeException) {
            throw invalidMonth(value)
        }
    }

    private fun invalidMonth(value: String) = BadRequestException("Invalid month '$value', expected YYYY-MM")
}
