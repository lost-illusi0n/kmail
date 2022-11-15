package dev.sitar.kmail.smtp.agent

import dev.sitar.kio.async.writers.toAsyncWriter
import dev.sitar.kio.buffers.buffer
import dev.sitar.kmail.smtp.InternetMessage
import dev.sitar.kmail.smtp.MailCommand
import dev.sitar.kmail.smtp.agent.transports.client.PlainTextSmtpTransportClient
import dev.sitar.kmail.smtp.agent.transports.client.SmtpsTransportClient
import dev.sitar.kmail.smtp.agent.transports.server.PlainTextSmtpServerTransportClient
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpServerReader
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.toAsyncByteChannelWriter
import dev.sitar.kmail.smtp.io.toAsyncByteReadChannelReader
import io.ktor.utils.io.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

const val HOSTNAME = "linux.org"

// TODO: use kotlin-logging
// TODO: ktor doesnt support common tls server. implement it in jvm at least manually. it is important
suspend fun main(): Unit = coroutineScope {
    val channel = ByteChannel()
    channel.toAsyncByteChannelWriter().asAsyncSmtpClientWriter().writeCommand(MailCommand("lost@2d.gay"))
    println(channel.toAsyncByteReadChannelReader().asAsyncSmtpClientReader().readUtf8UntilSmtpEnding())

    println("STARTING KMAIL SMTP AGENT.")

    val smtpSubmissionAgent = SubmissionAgent.withHostname(HOSTNAME, PlainTextSmtpServerTransportClient)

    val transferAgent = TransferAgent.fromOutgoingMessages(HOSTNAME, smtpSubmissionAgent.incomingMail, PlainTextSmtpTransportClient)

    smtpSubmissionAgent.start()
}