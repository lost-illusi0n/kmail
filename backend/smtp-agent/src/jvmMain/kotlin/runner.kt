package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.agent.transports.server.PlainTextSmtpServerTransportClient
import kotlinx.coroutines.coroutineScope

const val HOSTNAME = "linux.org"

// TODO: use kotlin-logging
// TODO: ktor doesnt support common tls server. implement it in jvm at least manually. it is important
suspend fun main(): Unit = coroutineScope {
    println("STARTING KMAIL SMTP AGENT.")

    val smtpSubmissionAgent = SubmissionAgent.withHostname(HOSTNAME, PlainTextSmtpServerTransportClient)

    val transferAgent = TransferAgent.fromOutgoingMessages(HOSTNAME, smtpSubmissionAgent.incomingMail)

    smtpSubmissionAgent.start()
}