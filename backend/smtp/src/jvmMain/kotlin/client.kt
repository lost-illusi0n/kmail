package dev.sitar.kmail.smtp

import dev.sitar.kmail.message.headers.from
import dev.sitar.kmail.message.headers.originalDate
import dev.sitar.kmail.message.headers.subject
import dev.sitar.kmail.message.headers.toRcpt
import dev.sitar.kmail.message.message
import dev.sitar.kmail.smtp.io.*
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.toAsyncByteChannelWriter
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.UtcOffset
import kotlin.system.exitProcess

private const val SMTP_SERVER = "localhost"
private const val OUR_HOST = "linux.org"
private const val RECIPIENT = "marco@sitar.dev"

private suspend fun main(): Unit = coroutineScope {
    val transport = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(SMTP_SERVER, 587)

    val reader = transport.openReadChannel().toAsyncByteReadChannelReader().asAsyncSmtpClientReader()
    val writer = transport.openWriteChannel().toAsyncByteChannelWriter().asAsyncSmtpClientWriter()

    got<GreetReply>(reader)

    send(writer, EhloCommand(OUR_HOST))
    got<EhloReply>(reader)

    send(writer, MailCommand("zuzana@spoofed.com"))
    got<OkReply>(reader)

    send(writer, RecipientCommand(RECIPIENT))
    got<OkReply>(reader)

    send(writer, DataCommand)
    got<StartMailInputReply>(reader)

    send(writer, MailInputCommand(message {
        headers {
            +from("Marco <marco@sitar.com>")
            +toRcpt("<marco@sitar.dev>")
            +originalDate(Clock.System.now(), UtcOffset(-5))
            +subject("message format rewrite")
        }

        body {
            line("hello")
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