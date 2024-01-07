package dev.sitar.kmail.agents.smtp.connections

import dev.sitar.kmail.agents.smtp.transports.client.SmtpClientTransport
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.frames.reply.EhloReply
import dev.sitar.kmail.smtp.frames.reply.SmtpReply
import dev.sitar.kmail.smtp.frames.reply.SmtpReplyCode
import dev.sitar.kmail.smtp.frames.reply.SmtpReplyDeserializer
import dev.sitar.kmail.utils.todo
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

abstract class ClientConnection(
    val transport: SmtpClientTransport,
    val domain: Domain
) {
    val ehloObjective: EhloObjective by lazy { EhloObjective(this, domain) }
    abstract val objectives: Set<ClientObjective>

    suspend fun start(): ClientObjective.Result {
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
                todo("failed objective")
            }
        }

        transport.send(QuitCommand)
        recv()

        logger.info { "Connection ${transport.connection.value.remote} has terminated." }

        transport.close()

        return runner.lastResult
    }

    suspend fun recv(): SmtpReply.Raw {
        val reply = transport.recv()

        return when (reply.code) {
            is SmtpReplyCode.PositiveCompletion, is SmtpReplyCode.PositiveIntermediate -> reply
            is SmtpReplyCode.TransientNegative -> todo()
            is SmtpReplyCode.PermanentNegative -> todo()
        }
    }
}

class ObjectiveRunner(val objectives: List<ClientObjective>) {
    var currentObjective = 0
    var nextObjective = 1

    val isFinished get() = objectives.getOrNull(nextObjective) == null

    var lastResult: ClientObjective.Result = ClientObjective.Result.Okay

    inline fun <reified T: ClientObjective> next() {
        nextObjective = objectives.indexOfFirst { it is T }
    }

    suspend fun step() {
        lastResult = with(current()) { begin() }

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

    sealed interface Result {
        object Okay: Result
        object RetryLater: Result
        object Bad: Result
    }

    suspend fun ObjectiveRunner.begin(): Result
}

class EhloObjective(override val client: ClientConnection, val domain: Domain): ClientObjective {
    lateinit var capabilities: List<String>

    override suspend fun ObjectiveRunner.begin(): ClientObjective.Result {
        client.transport.send(EhloCommand(domain))

        val ehlo = client.recv() deserializeAs EhloReply

        capabilities = ehlo.capabilities

        return ClientObjective.Result.Okay
    }
}

class SecureObjective(override val client: ClientConnection, requiresEncryption: Boolean): ClientObjective {
    private val logger = KotlinLogging.logger { }

    override val isOptional: Boolean = !requiresEncryption

    override suspend fun ObjectiveRunner.begin(): ClientObjective.Result {
        if (client.transport.isSecure) return ClientObjective.Result.Okay

        if ("STARTTLS" !in client.ehloObjective.capabilities) {
            if (!isOptional) todo("encryption pls")
            logger.debug { "Continuing without encryption." }
            return ClientObjective.Result.Okay
        }

        client.transport.send(StartTlsCommand)

        client.recv()

        logger.debug { "Starting TLS negotiations. ${System.currentTimeMillis()}" }
        client.transport.secure()
        logger.debug { "Upgraded connection to TLS. ${System.currentTimeMillis()}"}

        next<EhloObjective>()

        return ClientObjective.Result.Okay
    }
}

class MailObjective(override val client: ClientConnection, val message: InternetMessage, val rcpts: List<Path>): ClientObjective {
    override suspend fun ObjectiveRunner.begin(): ClientObjective.Result {
        client.transport.send(MailCommand(message.envelope.originatorAddress))
        if (client.recv().code is SmtpReplyCode.TransientNegative) return ClientObjective.Result.RetryLater

        rcpts.forEach {
            client.transport.send(RecipientCommand(it))
            client.recv()
        }

        client.transport.send(DataCommand)
        if (client.recv().code is SmtpReplyCode.TransientNegative) return ClientObjective.Result.RetryLater

        client.transport.sendMessage(message.message)
        if (client.recv().code is SmtpReplyCode.TransientNegative) return ClientObjective.Result.RetryLater

        return ClientObjective.Result.Okay
    }
}

class QuitObjective(override val client: ClientConnection): ClientObjective {
    override suspend fun ObjectiveRunner.begin(): ClientObjective.Result.Okay {
        client.transport.send(QuitCommand)
        client.recv()

        return ClientObjective.Result.Okay
    }
}

private infix fun <T: SmtpReply> SmtpReply.Raw.deserializeAs(deserializer: SmtpReplyDeserializer<out T>): T {
    return deserializer.deserialize(this)!!
}