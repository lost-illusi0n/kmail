package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.message.headers.from
import dev.sitar.kmail.message.headers.originalDate
import dev.sitar.kmail.message.headers.subject
import dev.sitar.kmail.message.headers.toRcpt
import dev.sitar.kmail.message.message
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.frames.replies.SmtpReply
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
private const val RECIPIENT = "<example@spoofed.com>"

private suspend fun main(): Unit = coroutineScope {
    val transport = DefaultSubmissionSmtpConnector().connect("0.0.0.0") ?: error("could not connect")

    val reader = transport.reader.asAsyncSmtpClientReader()
    val writer = transport.writer.asAsyncSmtpClientWriter()

    got<SmtpReply.PositiveCompletion>(reader)

    send(writer, EhloCommand(OUR_HOST))
    got<SmtpReply.PositiveCompletion>(reader)

    send(writer, MailCommand("example@spoofed.com"))
    got<SmtpReply.PositiveCompletion>(reader)

    send(writer, RecipientCommand(RECIPIENT))
    got<SmtpReply.PositiveCompletion>(reader)

    send(writer, DataCommand)
    got<SmtpReply.PositiveIntermediate>(reader)

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
    got<SmtpReply.PositiveCompletion>(reader)

    send(writer, QuitCommand)
    got<SmtpReply.PositiveCompletion>(reader)

    exitProcess(0)
}

// TODO: temporary writer/reader functions, replace with client (in agent module maybe)
private suspend inline fun <reified C: SmtpReply<C>> got(reader: AsyncSmtpClientReader) {
    val reply = reader.readSmtpReply()
    require(reply is C)
    println("SUBMISSION CLIENT <<< $reply")
}


private suspend inline fun <reified T: SmtpCommand> send(writer: AsyncSmtpClientWriter, command: T) {
    writer.writeCommand(command)
    println("SUBMISSION CLIENT >>> $command")
}