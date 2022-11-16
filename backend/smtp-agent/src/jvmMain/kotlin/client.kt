package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.message.headers.from
import dev.sitar.kmail.message.headers.originalDate
import dev.sitar.kmail.message.headers.subject
import dev.sitar.kmail.message.headers.toRcpt
import dev.sitar.kmail.message.message
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.toAsyncByteChannelWriter
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.UtcOffset
import kotlin.system.exitProcess

private const val OUR_HOST = "linux.org"
private const val RECIPIENT = "<example@exampl.com>"

private suspend fun main(): Unit = coroutineScope {
    val transport = DefaultSubmissionSmtpConnector().connect("0.0.0.0") ?: error("could not connect")

    val reader = transport.reader.asAsyncSmtpClientReader()
    val writer = transport.writer.asAsyncSmtpClientWriter()

    got<GreetReply>(reader)

    send(writer, EhloCommand(OUR_HOST))
    got<EhloReply>(reader)

    send(writer, MailCommand("Example <example@example.gay>"))
    got<OkReply>(reader)

    send(writer, RecipientCommand(RECIPIENT))
    got<OkReply>(reader)

    send(writer, DataCommand)
    got<StartMailInputReply>(reader)

    send(writer, MailInputCommand(message {
        headers {
            +from("Example <example@example.com>")
            +toRcpt("$RECIPIENT")
            +originalDate(Clock.System.now(), UtcOffset(-5))
            +subject("example")
        }

        body {
            line("example")
        }
    }))
    got<OkReply>(reader)

    send(writer, QuitCommand)
    got<OkReply>(reader)

    exitProcess(0)
}

// TODO: temporary writer/reader functions, replace with client (in agent module maybe)
private suspend inline fun <reified T: SmtpReply> got(reader: AsyncSmtpClientReader) {
    val reply = reader.readSmtpReply<T>()
    println("SUBMISSION CLIENT <<< $reply")
}


private suspend inline fun <reified T: SmtpCommand> send(writer: AsyncSmtpClientWriter, command: T) {
    writer.writeCommand(command)
    println("SUBMISSION CLIENT >>> $command")
}