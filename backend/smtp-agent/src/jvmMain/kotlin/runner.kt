package dev.sitar.kmail.smtp.agent

import kotlinx.coroutines.coroutineScope

const val HOSTNAME = "linux.org"

// TODO: use kotlin-logging
suspend fun main(): Unit = coroutineScope {
    println("STARTING KMAIL SMTP AGENT.")

    val submissionAgent = SubmissionAgent.withHostname(HOSTNAME)
    val transferAgent = TransferAgent.fromOutgoingMessages(HOSTNAME, submissionAgent.incomingMail)

    submissionAgent.start()
}