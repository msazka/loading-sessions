package com.dcs.charging.invoice

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Clock
import java.time.Duration
import java.time.Instant

@SpringBootTest(properties = ["spring.datasource.url=jdbc:h2:mem:charging-invoice;DB_CLOSE_DELAY=-1"])
@AutoConfigureMockMvc
class InvoiceApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    lateinit var clock: Clock

    @Test
    fun `invoice lists the line and totals from the ticket example`() {
        val id = completedSession("i-example", "AC_STANDARD", "20.000", "2026-11-10T12:00:00Z")

        invoice("i-example", "2026-11").andExpect {
            status { isOk() }
            jsonPath("$.customerId") { value("i-example") }
            jsonPath("$.month") { value("2026-11") }
            jsonPath("$.lines.length()") { value(1) }
            jsonPath("$.lines[0].sessionId") { value(id) }
            jsonPath("$.lines[0].endedAt") { value("2026-11-10T12:00:00Z") }
            jsonPath("$.lines[0].tariffCode") { value("AC_STANDARD") }
            jsonPath("$.lines[0].energyKwh") { value(20.0) }
            jsonPath("$.lines[0].cost") { value(11.80) }
            jsonPath("$.totalEnergyKwh") { value(20.0) }
            jsonPath("$.totalGross") { value(11.80) }
            jsonPath("$.vatRate") { value(0.19) }
            jsonPath("$.totalNet") { value(9.92) }
            jsonPath("$.totalVat") { value(1.88) }
        }
    }

    @Test
    fun `VAT is calculated on the total, not per line`() {
        completedSession("i-sum", "AC_STANDARD", "20.000", "2026-11-10T12:00:00Z") // 11.80
        completedSession("i-sum", "SUBSCRIPTION_M", "20.000", "2026-11-11T12:00:00Z") // 9.35

        // Rounding each line first would give a net of 9.92 + 7.86 = 17.78.
        invoice("i-sum", "2026-11").andExpect {
            jsonPath("$.totalEnergyKwh") { value(40.0) }
            jsonPath("$.totalGross") { value(21.15) }
            jsonPath("$.totalNet") { value(17.77) }
            jsonPath("$.totalVat") { value(3.38) }
        }
    }

    @Test
    fun `lines are ordered by end time`() {
        val later = completedSession("i-order", "AC_STANDARD", "1.000", "2026-11-20T12:00:00Z")
        val earlier = completedSession("i-order", "AC_STANDARD", "1.000", "2026-11-05T12:00:00Z")

        invoice("i-order", "2026-11").andExpect {
            jsonPath("$.lines[0].sessionId") { value(earlier) }
            jsonPath("$.lines[1].sessionId") { value(later) }
        }
    }

    @Test
    fun `lines with the same end time have a stable order`() {
        val ids = List(4) { completedSession("i-tie", "AC_STANDARD", "1.000", "2026-11-05T12:00:00Z") }

        invoice("i-tie", "2026-11").andExpect {
            ids.sorted().forEachIndexed { index, id ->
                jsonPath("$.lines[$index].sessionId") { value(id) }
            }
        }
    }

    @Test
    fun `month starts at midnight Berlin time in winter, not UTC`() {
        // CET is UTC+1, so Berlin's November starts at 23:00 UTC on 31 October.
        completedSession("i-winter", "AC_STANDARD", "1.000", "2026-10-31T22:59:59Z")
        completedSession("i-winter", "AC_STANDARD", "2.000", "2026-10-31T23:00:00Z")

        assertSingleLine("i-winter", "2026-10", energyKwh = 1.0)
        assertSingleLine("i-winter", "2026-11", energyKwh = 2.0)
    }

    @Test
    fun `month starts at midnight Berlin time in summer, not UTC`() {
        // CEST is UTC+2, so Berlin's July starts at 22:00 UTC on 30 June.
        completedSession("i-summer", "AC_STANDARD", "1.000", "2026-06-30T21:59:59Z")
        completedSession("i-summer", "AC_STANDARD", "2.000", "2026-06-30T22:00:00Z")

        assertSingleLine("i-summer", "2026-06", energyKwh = 1.0)
        assertSingleLine("i-summer", "2026-07", energyKwh = 2.0)
    }

    @Test
    fun `a month that contains the clock change uses the right offset at each end`() {
        // October 2026 starts in CEST (UTC+2) and ends in CET (UTC+1).
        completedSession("i-dst", "AC_STANDARD", "1.000", "2026-09-30T21:59:59Z") // September
        completedSession("i-dst", "AC_STANDARD", "2.000", "2026-09-30T22:00:00Z") // October, first second
        completedSession("i-dst", "AC_STANDARD", "4.000", "2026-10-31T22:59:59Z") // October, last second
        completedSession("i-dst", "AC_STANDARD", "8.000", "2026-10-31T23:00:00Z") // November

        invoice("i-dst", "2026-10").andExpect {
            jsonPath("$.lines.length()") { value(2) }
            jsonPath("$.totalEnergyKwh") { value(6.0) }
        }
    }

    @Test
    fun `new year falls in the next year in Berlin time`() {
        completedSession("i-year", "AC_STANDARD", "2.000", "2026-12-31T23:00:00Z")

        assertSingleLine("i-year", "2027-01", energyKwh = 2.0)
        invoice("i-year", "2026-12").andExpect { jsonPath("$.lines.length()") { value(0) } }
    }

    @Test
    fun `a session belongs to the month in which it ended`() {
        completedSession(
            "i-span", "AC_STANDARD", "3.000", "2026-11-01T00:30:00Z", duration = Duration.ofHours(3),
        )

        invoice("i-span", "2026-10").andExpect { jsonPath("$.lines.length()") { value(0) } }
        assertSingleLine("i-span", "2026-11", energyKwh = 3.0)
    }

    @Test
    fun `active sessions and other customers are left out`() {
        completedSession("i-mine", "AC_STANDARD", "1.000", "2026-11-10T12:00:00Z")
        completedSession("i-other", "AC_STANDARD", "9.000", "2026-11-10T12:00:00Z")
        `when`(clock.instant()).thenReturn(Instant.parse("2026-11-10T13:00:00Z"))
        startSession("i-mine", "AC_STANDARD")

        assertSingleLine("i-mine", "2026-11", energyKwh = 1.0)
    }

    @Test
    fun `no sessions gives an empty invoice with zero totals`() {
        invoice("i-nobody", "2026-11").andExpect {
            status { isOk() }
            jsonPath("$.customerId") { value("i-nobody") }
            jsonPath("$.month") { value("2026-11") }
            jsonPath("$.lines.length()") { value(0) }
            jsonPath("$.totalEnergyKwh") { value(0.0) }
            jsonPath("$.totalGross") { value(0.0) }
            jsonPath("$.vatRate") { value(0.19) }
            jsonPath("$.totalNet") { value(0.0) }
            jsonPath("$.totalVat") { value(0.0) }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["2026-13", "2026-00", "abc", "2026-1", "26-11", "+12026-11", "2026-11-01", "202611"])
    fun `invalid month returns 400`(month: String) {
        invoice("i-invalid", month).andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { exists() }
        }
    }

    private fun invoice(customerId: String, month: String): ResultActionsDsl =
        mockMvc.get("/api/customers/{customerId}/invoices/{month}", customerId, month)

    private fun assertSingleLine(customerId: String, month: String, energyKwh: Double) {
        invoice(customerId, month).andExpect {
            status { isOk() }
            jsonPath("$.lines.length()") { value(1) }
            jsonPath("$.lines[0].energyKwh") { value(energyKwh) }
        }
    }

    private fun completedSession(
        customerId: String,
        tariffCode: String,
        energyKwh: String,
        endedAt: String,
        duration: Duration = Duration.ofHours(1),
    ): String {
        val end = Instant.parse(endedAt)
        `when`(clock.instant()).thenReturn(end.minus(duration))
        val id = startSession(customerId, tariffCode)

        `when`(clock.instant()).thenReturn(end)
        mockMvc.post("/api/sessions/$id/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "energyKwh": $energyKwh }"""
        }.andExpect {
            status { isOk() }
        }
        return id
    }

    private fun startSession(customerId: String, tariffCode: String): String {
        val json = mockMvc.post("/api/sessions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "customerId": "$customerId", "chargePointId": "DE*DCS*E0001", "tariffCode": "$tariffCode" }"""
        }.andReturn().response.contentAsString
        return objectMapper.readTree(json)["id"].asText()
    }
}
