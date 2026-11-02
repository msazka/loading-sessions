package com.dcs.charging.session

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/sessions")
class ChargingSessionController(private val service: ChargingSessionService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun start(@Valid @RequestBody request: StartSessionRequest): SessionResponse =
        SessionResponse.from(service.start(request))

    @PostMapping("/{id}/stop")
    fun stop(@PathVariable id: UUID, @Valid @RequestBody request: StopSessionRequest): SessionResponse =
        SessionResponse.from(service.stop(id, request))

    @GetMapping
    fun list(@RequestParam(required = false) customerId: String?): List<SessionResponse> =
        service.list(customerId).map(SessionResponse::from)
}
