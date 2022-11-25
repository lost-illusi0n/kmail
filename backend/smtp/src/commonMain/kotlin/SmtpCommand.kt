package dev.sitar.kmail.smtp

import dev.sitar.kio.buffers.DefaultBufferPool
import dev.sitar.kio.fullSlice
import dev.sitar.kio.use
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.io.readStringUtf8
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8
import kotlin.math.max
import kotlin.system.exitProcess

public enum class SmtpCommandTag(public val serializer: SmtpCommandSerializer<*>) {
    Helo(HeloCommand.Serializer),
    Ehlo(EhloCommand.Serializer),
    Mail(MailCommand.Serializer),
    Rcpt(RecipientCommand.Serializer),
    Data(DataCommand.Serializer),
    Quit(QuitCommand.Serializer),
    StartTls(StartTlsCommand.Serializer),
    Auth(AuthenticationCommand.Serializer);

    public companion object {
        public fun fromTag(tag: String): SmtpCommandTag? {
            return values().find { it.name.lowercase() == tag.lowercase() }
        }
    }
}

// TODO: make these easier to serialize
public sealed interface SmtpCommand {
    public val tag: SmtpCommandTag
}

public interface SmtpCommandSerializer<T: SmtpCommand> {
    public suspend fun serialize(command: T, output: AsyncSmtpWriter)

    public suspend fun deserialize(input: AsyncSmtpReader): T
}

public data class HeloCommand(val domain: String) : SmtpCommand {
    override val tag: SmtpCommandTag = SmtpCommandTag.Helo

    public object Serializer: SmtpCommandSerializer<HeloCommand> {
        public override suspend fun serialize(command: HeloCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8(command.domain)
            output.endLine()
        }

        public override suspend fun deserialize(input: AsyncSmtpReader): HeloCommand {
            return HeloCommand(input.readUtf8UntilSmtpEnding())
        }
    }
}

public data class EhloCommand(val data: String) : SmtpCommand {
    override val tag: SmtpCommandTag = SmtpCommandTag.Ehlo

    public object Serializer : SmtpCommandSerializer<EhloCommand> {
        public override suspend fun serialize(command: EhloCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8(command.data)
            output.endLine()
        }

        public override suspend fun deserialize(input: AsyncSmtpReader): EhloCommand {
            return EhloCommand(input.readUtf8UntilSmtpEnding())
        }
    }
}

public data class MailCommand(val from: Path /* params */) : SmtpCommand {
    override val tag: SmtpCommandTag = SmtpCommandTag.Mail

    public object Serializer : SmtpCommandSerializer<MailCommand> {
        public override suspend fun serialize(command: MailCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8("FROM:${command.from.asText()}")
            output.endLine()
        }

        public override suspend fun deserialize(input: AsyncSmtpReader): MailCommand {
            if (input.readStringUtf8(5) != "FROM:") TODO("incorrect syntax")

            val path = Path.fromText(input.readUtf8UntilSmtpEnding()) ?: TODO("incorrect syntax")

            return MailCommand(path)
        }
    }
}

public data class RecipientCommand(val to: Path /* params */) : SmtpCommand {
    override val tag: SmtpCommandTag = SmtpCommandTag.Rcpt

    public object Serializer : SmtpCommandSerializer<RecipientCommand> {
        public override suspend fun serialize(command: RecipientCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8("TO:${command.to.asText()}")
            output.endLine()
        }

        public override suspend fun deserialize(input: AsyncSmtpReader): RecipientCommand {
            if (input.readStringUtf8(3) != "TO:") TODO("incorrect syntax")

            val path = Path.fromText(input.readUtf8UntilSmtpEnding()) ?: TODO("incorrect syntax")

            return RecipientCommand(path)
        }
    }
}

public object DataCommand : SmtpCommand {
    override val tag: SmtpCommandTag = SmtpCommandTag.Data

    public object Serializer : SmtpCommandSerializer<DataCommand> {
        override suspend fun serialize(command: DataCommand, output: AsyncSmtpWriter) {
            output.endLine()
        }

        public override suspend fun deserialize(input: AsyncSmtpReader): DataCommand {
            return DataCommand
        }
    }

    override fun toString(): String {
        return "DataCommand"
    }
}

public object QuitCommand : SmtpCommand {
    override val tag: SmtpCommandTag = SmtpCommandTag.Quit

    public object Serializer : SmtpCommandSerializer<QuitCommand> {
        override suspend fun serialize(command: QuitCommand, output: AsyncSmtpWriter) {
            output.endLine()
        }

        public override suspend fun deserialize(input: AsyncSmtpReader): QuitCommand {
            return QuitCommand
        }
    }

    override fun toString(): String {
        return "QuitCommand"
    }
}

public object StartTlsCommand : SmtpCommand {
    override val tag: SmtpCommandTag = SmtpCommandTag.StartTls

    public object Serializer : SmtpCommandSerializer<StartTlsCommand> {
        override suspend fun serialize(command: StartTlsCommand, output: AsyncSmtpWriter) {
            output.endLine()
        }

        public override suspend fun deserialize(input: AsyncSmtpReader): StartTlsCommand {
            return StartTlsCommand
        }
    }

    override fun toString(): String {
        return "StartTlsCommand"
    }
}

public data class AuthenticationCommand(val mechanism: String, val response: SaslMechanism?): SmtpCommand {
    override val tag: SmtpCommandTag = SmtpCommandTag.Auth

    public object Serializer : SmtpCommandSerializer<AuthenticationCommand> {
        public override suspend fun serialize(authentication: AuthenticationCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8(authentication.mechanism)
            authentication.response?.let { output.writeStringUtf8(" ${authentication.response.encode()}") }
            output.endLine()
        }

        public override suspend fun deserialize(input: AsyncSmtpReader): AuthenticationCommand {
            val line = input.readUtf8UntilSmtpEnding()

            return when (val index = line.indexOf(' ')) {
                -1 -> AuthenticationCommand(line, null)
                else -> {
                    val mechanism = line.substring(0, index)
                    val encoded = line.substring(index + 1)

                    val decoded = when (mechanism) {
                        "PLAIN" -> decodePlainSaslMechanism(encoded)
                        else -> error("unsupported sasl mechanism: $mechanism")
                    }

                    AuthenticationCommand(line.substring(0, index), decoded)
                }
            }
        }
    }
}