package dev.sitar.kmail.agents.smtp.connections

import dev.sitar.kmail.agents.smtp.transports.client.SmtpClientTransport
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.frames.reply.EhloReply
import dev.sitar.kmail.smtp.frames.reply.SmtpReply
import dev.sitar.kmail.smtp.frames.reply.SmtpReplyCode
import dev.sitar.kmail.smtp.frames.reply.SmtpReplyDeserializer
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

abstract class ClientConnection(
    val transport: SmtpClientTransport,
    val domain: Domain
) {
    val ehloObjective: EhloObjective by lazy { EhloObjective(this, domain) }
    abstract val objectives: Set<ClientObjective>

    suspend fun start() {
        // initial greet
        require(transport.recv().code is SmtpReplyCode.PositiveCompletion)

        val allObjectives = listOf(
            ehloObjective,
            *objectives.toTypedArray(),
            QuitObjective(this)
        )

        val runner = ObjectiveRunner(allObjectives)

        while (!runner.isFinished) {
            try {
                runner.step()
            } catch (e: Exception) {
                TODO("failed objective")
            }
        }

        transport.send(QuitCommand)
        recv()

        logger.info { "Connection ${transport.connection.value.remote} has terminated." }

        transport.close()
    }

    suspend fun recv(): SmtpReply.Raw {
        val reply = transport.recv()

        return when (reply.code) {
            is SmtpReplyCode.PositiveCompletion, is SmtpReplyCode.PositiveIntermediate -> reply
            is SmtpReplyCode.TransientNegative -> TODO()
            is SmtpReplyCode.PermanentNegative -> TODO()
        }
    }
}

class ObjectiveRunner(val objectives: List<ClientObjective>) {
    var currentObjective = 0
    var nextObjective = 1

    val isFinished get() = objectives.getOrNull(nextObjective) == null

    inline fun <reified T: ClientObjective> next() {
        nextObjective = objectives.indexOfFirst { it is T }
    }

    suspend fun step() {
        with(current()) { begin() }

        currentObjective = nextObjective
        nextObjective++
    }

    fun current(): ClientObjective {
        return objectives[currentObjective]
    }
}

interface ClientObjective {
    val client: ClientConnection

    val isOptional: Boolean get() = true

    suspend fun ObjectiveRunner.begin()
}

class EhloObjective(override val client: ClientConnection, val domain: Domain): ClientObjective {
    lateinit var capabilities: List<String>

    override suspend fun ObjectiveRunner.begin() {
        client.transport.send(EhloCommand(domain))

        val ehlo = client.recv() deserializeAs EhloReply

        capabilities = ehlo.capabilities
    }
}

class SecureObjective(override val client: ClientConnection, requiresEncryption: Boolean): ClientObjective {
    private val logger = KotlinLogging.logger { }

    override val isOptional: Boolean = !requiresEncryption

    override suspend fun ObjectiveRunner.begin() {
        if (client.transport.isSecure) return

        if ("STARTTLS" !in client.ehloObjective.capabilities) {
            if (!isOptional) TODO("encryption pls")
            logger.debug { "Continuing without encryption." }
            return
        }

        client.transport.send(StartTlsCommand)

        client.recv()

        logger.debug { "Starting TLS negotiations. ${System.currentTimeMillis()}" }
        client.transport.secure()
        logger.debug { "Upgraded connection to TLS. ${System.currentTimeMillis()}"}

        next<EhloObjective>()
    }
}

class MailObjective(override val client: ClientConnection, val message: InternetMessage, val rcpt: Path): ClientObjective {
    override suspend fun ObjectiveRunner.begin() {
        client.transport.send(MailCommand(message.envelope.originatorAddress))
        client.recv()

        client.transport.send(RecipientCommand(rcpt))
        client.recv()

        client.transport.send(DataCommand)
        client.recv()

        client.transport.sendMessage(message.message)
        client.recv()
    }
}

class QuitObjective(override val client: ClientConnection): ClientObjective {
    private val logger = KotlinLogging.logger { }

    override suspend fun ObjectiveRunner.begin() {
        client.transport.send(QuitCommand)
        client.recv()
    }
}

private infix fun <T: SmtpReply> SmtpReply.Raw.deserializeAs(deserializer: SmtpReplyDeserializer<out T>): T {
    return deserializer.deserialize(this)!!
}