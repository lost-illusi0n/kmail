package dev.sitar.kmail.smtp

import dev.sitar.kio.buffers.DefaultBufferPool
import dev.sitar.kio.fullSlice
import dev.sitar.kio.use
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8
import kotlin.math.max

// TODO: make these easier to serialize
public sealed interface SmtpCommand {
    public val discriminator: String
}

// TODO: this should be data. they shouldnt handle writing their identifiers

public data class HeloCommand(val domain: String) : SmtpCommand {
    override val discriminator: String = "HELO"

    public object Serializer {
        public suspend fun serialize(command: HeloCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8(command.domain)
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): HeloCommand {
            require(input.readIsFinal())
            return HeloCommand(input.readUtf8UntilSmtpEnding())
        }
    }
}

public data class EhloCommand(val data: String) : SmtpCommand {
    override val discriminator: String = "EHLO"

    public object Serializer {
        public suspend fun serialize(command: EhloCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8(command.data)
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): EhloCommand {
            require(input.readIsFinal())

            return EhloCommand(input.readUtf8UntilSmtpEnding())
        }
    }
}

// TODO: parse and check to and from. there is a syntax they should follow
public data class MailCommand(val from: String /* params */) : SmtpCommand {
    override val discriminator: String = "MAIL"

    public object Serializer {
        public suspend fun serialize(command: MailCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8("FROM:${command.from}")
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): MailCommand {
            require(input.readIsFinal())

            val fromRaw = input.readUtf8UntilSmtpEnding()

            if (!fromRaw.startsWith("FROM:")) TODO("incorrect syntax")

            return MailCommand(fromRaw.drop(5))
        }
    }
}

public data class RecipientCommand(val to: String /* params */) : SmtpCommand {
    override val discriminator: String = "RCPT"

    public object Serializer {
        public suspend fun serialize(command: RecipientCommand, output: AsyncSmtpWriter) {
            output.writeIsFinal()
            output.writeStringUtf8("TO:${command.to}")
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): RecipientCommand {
            require(input.readIsFinal())

            val toRaw = input.readUtf8UntilSmtpEnding()

            if (!toRaw.startsWith("TO:")) TODO("incorrect syntax")

            return RecipientCommand(toRaw.drop(3))
        }
    }
}

public object DataCommand : SmtpCommand {
    override val discriminator: String = "DATA"

    public object Serializer {
        public suspend fun serialize(output: AsyncSmtpWriter) {
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): DataCommand {
            input.readUtf8UntilSmtpEnding()
            return DataCommand
        }
    }

    override fun toString(): String {
        return "DataCommand"
    }
}

public data class MailInputCommand(val message: Message) : SmtpCommand {
    public object Serializer {
        private val TERMINATING_SEQUENCE = "\r\n.\r\n".toByteArray().fullSlice()

        public suspend fun serialize(command: MailInputCommand, output: AsyncSmtpWriter) {
            output.writeStringUtf8(command.message.asText())
            output.write('.'.code.toByte())
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader) : MailInputCommand {
            val data: ByteArray = DefaultBufferPool.use(32) { resultBuffer ->
                for (byte in input) {
                    resultBuffer.write(byte)

                    val lastFive = resultBuffer[max(0, resultBuffer.writeIndex - 5)..resultBuffer.writeIndex]

                    // check if terminating sequence is found at the end of the result buffer
                    if (lastFive.contentEquals(TERMINATING_SEQUENCE)) {
                        resultBuffer.writeIndex -= 3 // the first <CRLF> is part of the body. don't need to remove it
                        return@use resultBuffer.toByteArray()
                    }
                }

                TODO("input stopped but terminating sequence not found after data")
            }

            return MailInputCommand(Message.fromText(data.decodeToString()))
        }
    }

    override val discriminator: String = ""
}

public object QuitCommand : SmtpCommand {
    public object Serializer {
        public suspend fun serialize(output: AsyncSmtpWriter) {
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): QuitCommand {
            input.readUtf8UntilSmtpEnding()
            return QuitCommand
        }
    }

    override val discriminator: String = "QUIT"

    override fun toString(): String {
        return "QuitCommand"
    }
}