package com.dcs.charging.session

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class ChargingSessionApiTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Test
    fun `starts a session`() {
        mockMvc.post("/api/sessions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "customerId": "c-1", "chargePointId": "DE*DCS*E0001", "tariffCode": "AC_STANDARD" }"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("ACTIVE") }
            jsonPath("$.tariffCode") { value("AC_STANDARD") }
        }
    }

    @Test
    fun `stopping a session sets energy and cost`() {
        val id = startSession(customerId = "c-2", tariffCode = "SUBSCRIPTION_M")

        mockMvc.post("/api/sessions/$id/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "energyKwh": 20.000 }"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("COMPLETED") }
            jsonPath("$.cost") { value(9.35) }
        }
    }

    @Test
    fun `unknown tariff returns 404`() {
        mockMvc.post("/api/sessions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "customerId": "c-3", "chargePointId": "DE*DCS*E0001", "tariffCode": "NOPE" }"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `negative energy returns 400`() {
        val id = startSession(customerId = "c-4", tariffCode = "AC_STANDARD")

        mockMvc.post("/api/sessions/$id/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "energyKwh": -1 }"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors.energyKwh") { exists() }
        }
    }

    @Test
    fun `stopping an unknown session returns 404`() {
        mockMvc.post("/api/sessions/${UUID.randomUUID()}/stop") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "energyKwh": 5 }"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `lists sessions of one customer`() {
        startSession(customerId = "c-5", tariffCode = "DC_FAST")

        mockMvc.get("/api/sessions") {
            param("customerId", "c-5")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].customerId") { value("c-5") }
        }
    }

    private fun startSession(customerId: String, tariffCode: String): String {
        val json = mockMvc.post("/api/sessions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "customerId": "$customerId", "chargePointId": "DE*DCS*E0001", "tariffCode": "$tariffCode" }"""
        }.andReturn().response.contentAsString
        return objectMapper.readTree(json)["id"].asText()
    }
}
