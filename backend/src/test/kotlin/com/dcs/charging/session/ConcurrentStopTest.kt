package com.dcs.charging.session

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Clock
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

/**
 * Forces the race instead of hoping for it. ChargingSessionService.stop reads the clock after its
 * status check, so a clock that blocks until both requests arrive guarantees that both have seen
 * the session as ACTIVE before either one commits.
 */
@SpringBootTest(properties = ["spring.datasource.url=jdbc:h2:mem:charging-race;DB_CLOSE_DELAY=-1"])
@AutoConfigureMockMvc
class ConcurrentStopTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @MockitoBean
    lateinit var clock: Clock

    private val now = Instant.parse("2026-11-02T12:00:00Z")

    @Test
    fun `only one of two simultaneous stops succeeds`() {
        `when`(clock.instant()).thenReturn(now)
        val id = startSession()

        val barrier = CyclicBarrier(2)
        `when`(clock.instant()).thenAnswer {
            barrier.await(10, TimeUnit.SECONDS)
            now
        }

        val executor = Executors.newFixedThreadPool(2)
        val results = try {
            listOf("20.000", "5.000")
                .map { energy -> executor.submit(Callable { stop(id, energy) }) }
                .map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(listOf(200, 409), results.map { it.first }.sorted())

        // The stored values must be the winner's, not a mix of both requests.
        val winner = objectMapper.readTree(results.first { it.first == 200 }.second)
        val stored = objectMapper.readTree(
            mockMvc.get("/api/sessions") { param("customerId", CUSTOMER) }
                .andReturn().response.contentAsString,
        )[0]
        assertEquals(0, winner["energyKwh"].decimalValue().compareTo(stored["energyKwh"].decimalValue()))
        assertEquals(0, winner["cost"].decimalValue().compareTo(stored["cost"].decimalValue()))
    }

    private fun startSession(): String {
        val json = mockMvc.post("/api/sessions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "customerId": "$CUSTOMER", "chargePointId": "DE*DCS*E0001", "tariffCode": "AC_STANDARD" }"""
        }.andReturn().response.contentAsString
        return objectMapper.readTree(json)["id"].asText()
    }

    private fun stop(id: String, energyKwh: String): Pair<Int, String> {
        val response = mockMvc.post("/api/sessions/$id/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "energyKwh": $energyKwh }"""
        }.andReturn().response
        return response.status to response.contentAsString
    }

    private companion object {
        const val CUSTOMER = "c-race"
    }
}
