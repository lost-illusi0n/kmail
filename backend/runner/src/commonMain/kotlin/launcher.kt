package dev.sitar.kmail.runner

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.job
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun run() = coroutineScope {
    println(KMAIL_ASCII)
    logger.info("Kmail is starting.")

    val submissionAgent = submission()
    val transferAgent = transfer(submissionAgent.incomingMail.consumeAsFlow())

    coroutineContext.job.join()
}