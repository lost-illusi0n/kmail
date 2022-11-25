package dev.sitar.kmail.smtp.agent

import dev.sitar.kio.async.readers.readFully
import dev.sitar.kmail.message.headers.*
import dev.sitar.kmail.message.message
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.agent.transports.client.TlsCapableSmtpSubmissionTransportClient
import dev.sitar.kmail.smtp.frames.replies.SmtpReply
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.reader.asAsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpClientWriter
import dev.sitar.kmail.smtp.io.smtp.writer.asAsyncSmtpClientWriter
import io.ktor.util.date.*
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.UtcOffset
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.system.exitProcess
//
//private const val OUR_HOST = "linux.org"
//private const val FROM = "<example@spoofed.com>"
//private const val RECIPIENT = "<example@spoofed.com>"
//
//private suspend fun main(): Unit = coroutineScope {
//    val sslContext = SSLContext.getInstance("TLS")
//
//    val keyStore = KeyStore.getInstance("JKS")
//    keyStore.load(javaClass.getResourceAsStream("/example.keystore"), "example".toCharArray())
//
//    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
//    keyManagerFactory.init(keyStore, "example".toCharArray())
//
//    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
//    trustManagerFactory.init(keyStore)
//
//    sslContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, null)
//
//    val transport = DefaultSubmissionSessionSmtpConnector(500, TlsCapableSmtpSubmissionTransportClient(sslContext)).connect("0.0.0.0") ?: error("could not connect")
//
//    println("CONNECTED")
//
//    var reader = transport.reader.asAsyncSmtpClientReader()
//    var writer = transport.writer.asAsyncSmtpClientWriter()
//
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    send(writer, EhloCommand(OUR_HOST))
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    send(writer, StartTlsCommand)
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    transport.upgradeToTls()
//    println("UPGRADED")
//    reader = transport.reader.asAsyncSmtpClientReader()
//    writer = transport.writer.asAsyncSmtpClientWriter()
//
//    send(writer, EhloCommand(OUR_HOST))
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    send(writer, AuthenticationCommand("PLAIN", PlainSaslMechanism(FROM, "example", "example")))
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    send(writer, MailCommand(FROM))
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    send(writer, RecipientCommand(RECIPIENT))
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    send(writer, DataCommand)
//    got<SmtpReply.PositiveIntermediate>(reader)
//
//    val message = message {
//        headers {
//            +from(FROM)
//            +toRcpt(RECIPIENT)
//            +originalDate(Clock.System.now(), UtcOffset(-5))
//            +subject("example")
//            +messageId("<${getTimeMillis()}@$OUR_HOST>")
//        }
//
//        body {
//            line("example")
//        }
//    }
//
//    writer.writeMessageData(message)
//    println("SUBMISSION CLIENT >>> $message")
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    send(writer, QuitCommand)
//    got<SmtpReply.PositiveCompletion>(reader)
//
//    exitProcess(0)
//}
//
//private suspend inline fun <reified C: SmtpReply<C>> got(reader: AsyncSmtpClientReader) {
//    val reply = reader.readSmtpReply()
//    println("SUBMISSION CLIENT <<< $reply")
//    require(reply is C)
//}
//
//
//private suspend inline fun <reified T: SmtpCommand> send(writer: AsyncSmtpClientWriter, command: T) {
//    writer.writeCommand(command)
//    println("SUBMISSION CLIENT >>> $command")
//}