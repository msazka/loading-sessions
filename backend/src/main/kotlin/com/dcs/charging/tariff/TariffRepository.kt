package com.dcs.charging.tariff

import org.springframework.data.jpa.repository.JpaRepository

interface TariffRepository : JpaRepository<Tariff, Long> {

    fun findByCode(code: String): Tariff?
}
