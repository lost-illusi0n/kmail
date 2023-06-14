package dev.sitar.kmail.runner

import dev.sitar.dns.dnsResolver
import dev.sitar.dns.records.MXResourceRecord
import dev.sitar.dns.records.ResourceType
import dev.sitar.dns.transports.DnsServer
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun dns() {
    logger.info { "Starting DNS check." }
    val dnsResolver = dnsResolver()

    Config.domains.forEach {
        logger.info { "Checking $it." }

        dnsResolver.resolveRecursively(it.asString()) {
            qType = ResourceType.MX
        }.also { logger.info { it } }

        dnsResolver.resolveRecursively(it.asString()) {
            qType = ResourceType.TXT
        }.also { logger.info { it } }

        dnsResolver.resolveRecursively(it.asString()) {
            qType = ResourceType.A
        }.also { logger.info { it } }
    }
}