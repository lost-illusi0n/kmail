package dev.sitar.kmail.runner

import dev.sitar.kmail.agents.smtp.DefaultTransferSessionSmtpConnector
import dev.sitar.kmail.agents.smtp.transports.SMTP_TRANSFER_PORT
import dev.sitar.kmail.utils.connection.TlsCapableConnectionFactory
import dev.sitar.kmail.utils.server.TlsCapableServerSocketFactory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.concurrent.Future

//import dev.sitar.kmail.imap.agent.transports.TlsCapableImapServerTransportClient
//import dev.sitar.kmail.smtp.agent.transports.server.TlsCapableSmtpSubmissionServerTransportClient

suspend fun main(): Unit = coroutineScope {
//    System.setProperty("javax.net.debug", "all")

    val ssl = ssl()

//    val server = TlsCapableServerSocketFactory(ssl).bind(25)

//    launch {
//        var socket = server.accept()
//        println(socket.reader.readUtf8UntilLineEnd())
//        println(socket.reader.readUtf8UntilLineEnd())
//        socket = socket.secureAsServer()
//        println("secured as server")
//        println(socket.reader.readUtf8UntilLineEnd())
//    }
//
//    launch {
//        var connection = TlsCapableConnectionFactory(ssl, 25).connect("localhost")
//        connection.writer.writeStringUtf8("EHLO [0.0.0.0]\r\n")
//        connection.writer.writeStringUtf8("STARTTLS\r\n")
//        connection = connection.secureAsClient()
//        println("secured as client")
//        connection.writer.writeStringUtf8("EHLO [0.0.0.0]")
//    }
//
//    coroutineContext[Job]!!.join()
//    return@coroutineScope

//    launch {
//        val socket = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(25)).accept().wait()
//
//        val conn = AsyncJvmSmtpConnection(socket, ssl, false)
//        println(conn.reader.readInt())
//        println(conn.reader.readInt())
//        conn.secureAsClient()
//        println(conn.reader.readInt())
//    }
//
//
//    delay(2000)
//
//
//    val client = aSocket(ActorSelectorManager(coroutineContext)).tcp().connect("localhost", 25)
//    val writer = client.openWriteChannel(autoFlush = true)
//    writer.writeInt(24)
//    delay(1000)
//    writer.writeInt(22)
//    client.tls(coroutineContext)
//    writer.writeInt(24)
//
//    delay(1000 * 60)
//    return@coroutineScope

//    val imapServerTransportClient = TlsCapableImapServerTransportClient(ssl)
//    val smtpServerTransportClient = TlsCapableSmtpSubmissionServerTransportClient(ssl)
    val socketFactory = TlsCapableServerSocketFactory(ssl)


//    run(imapServerTransportClient, smtpServerTransportClient)
    // TODO: make self-signed easier to work with??
    run(DefaultTransferSessionSmtpConnector(500, TlsCapableConnectionFactory(ssl, SMTP_TRANSFER_PORT)), socketFactory)
}

suspend fun <T> Future<T>.wait(): T {
    while(!isDone)
        delay(1) // or whatever you want your polling frequency to be
    return get()
}