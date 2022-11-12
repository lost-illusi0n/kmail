package dev.sitar.kmail.smtp

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.smtp.io.*
import dev.sitar.kmail.smtp.io.toAsyncByteChannelWriter
import dev.sitar.kmail.smtp.io.toAsyncByteReadChannelReader
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlin.system.exitProcess

private const val SMTP_SERVER = "smtp.example.com"
private const val OUR_HOST = "example.org"
private const val RECIPIENT = "example@example.com"
private suspend fun main(): Unit = coroutineScope {
    val transport = aSocket(SelectorManager(Dispatchers.Default)).tcp().connect(SMTP_SERVER, 25)

    val reader = transport.openReadChannel().toAsyncByteReadChannelReader().asAsyncSmtpReader()
    val writer = transport.openWriteChannel().toAsyncByteChannelWriter()

    got<GreetReply>(reader)

    send(writer, EhloCommand(OUR_HOST)) { EhloCommand.Serializer.serialize(it, this) }
    got<EhloReply>(reader)

    send(writer, MailCommand("zuzana@spoofed.com")) { MailCommand.Serializer.serialize(it, this) }
    got<OkReply>(reader)

    send(writer, RecipientCommand(RECIPIENT)) { RecipientCommand.Serializer.serialize(it, this) }
    got<OkReply>(reader)

    send(writer, DataCommand) { DataCommand.Serializer.serialize(this) }
    got<StartMailInputReply>(reader)

    send(writer, message(from = "Zuzana Caputova <zuzana@spoofed.com>", host = OUR_HOST) {
        to = RECIPIENT

        subject = "a kotlin subject"
        content = "hey, from kotlin."
    }) { MessageCommand.Serializer.serialize(it, this) }
    got<OkReply>(reader)

    send(writer, QuitCommand) { QuitCommand.Serializer.serialize(this) }
    got<OkReply>(reader)

    exitProcess(0)
}

// TODO: temporary writer/reader functions, replace with client (in agent module maybe)
private suspend inline fun <reified T: SmtpReply> got(reader: AsyncSmtpReader) {
    val reply = reader.readSmtpReply<T>()
    println("AGENT <<< $reply")
}

private suspend fun gotRest(reader: AsyncSmtpReader) {
    while (reader.openForRead) {
        println(reader.readUtf8UntilSmtpEnding())
    }
}


private inline fun <T: SmtpCommand> send(writer: AsyncByteWriteChannelWriter, command: T, serializer: AsyncWriter.(T) -> Unit) {
    println("AGENT >>> $command")
    serializer(writer, command)
    writer.flush()
}