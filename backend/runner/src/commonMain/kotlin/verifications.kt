package dev.sitar.kmail.runner

import dev.sitar.dns.dnsResolver
import dev.sitar.dns.records.ResourceType
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun dns() {
    logger.info { "Starting DNS check." }
    val dnsResolver = dnsResolver()

    Config.domains.forEach {
        logger.info { "Checking $it." }

        val records = dnsResolver.resolveRecursively(it.asString()) {
            qType = ResourceType.MX
        }

        println(records)
    }
}