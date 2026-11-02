package com.dcs.charging.tariff

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

data class TariffResponse(
    val code: String,
    val name: String,
    val pricePerKwh: BigDecimal,
    val sessionFee: BigDecimal,
)

@RestController
@RequestMapping("/api/tariffs")
class TariffController(private val tariffs: TariffRepository) {

    @GetMapping
    fun list(): List<TariffResponse> =
        tariffs.findAll().map { TariffResponse(it.code, it.name, it.pricePerKwh, it.sessionFee) }
}
