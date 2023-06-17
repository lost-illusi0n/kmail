package dev.sitar.kmail.runner

import dev.sitar.dns.Dns
import dev.sitar.dns.records.ResourceType
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun dns() {
    logger.info { "Starting DNS check." }

    Config.domains.forEach {
        logger.info { "Checking $it." }

        Dns.resolveRecursively(it.asString()) {
            qType = ResourceType.MX
        }.also { logger.info { it } }

        Dns.resolveRecursively(it.asString()) {
            qType = ResourceType.TXT
        }.also { logger.info { it } }

        Dns.resolveRecursively(it.asString()) {
            qType = ResourceType.A
        }.also { logger.info { it } }
    }
}