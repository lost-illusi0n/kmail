package dev.sitar.kmail.smtp

import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8

public const val SMTP_LINE_ENDING: String = "\r\n"

public sealed interface SmtpCommand

public data class HeloCommand(val domain: String): SmtpCommand {
    public object Serializer {
        public suspend fun serialize(command: HeloCommand, output: AsyncWriter) {
            output.writeStringUtf8("HELO")
            output.write(' '.code.toByte())
            output.writeStringUtf8(command.domain)
            output.writeStringUtf8(SMTP_LINE_ENDING)
        }
    }
}

public data class EhloCommand(val data: String): SmtpCommand {
    public object Serializer {
        public suspend fun serialize(command: EhloCommand, output: AsyncWriter) {
            output.writeStringUtf8("EHLO")
            output.write(' '.code.toByte())
            output.writeStringUtf8(command.data)
            output.writeStringUtf8(SMTP_LINE_ENDING)
        }
    }
}

public data class MailCommand(val from: String /* params */) : SmtpCommand {
    public object Serializer {
        public suspend fun serialize(command: MailCommand, output: AsyncWriter) {
            output.writeStringUtf8("MAIL")
            output.writeStringUtf8(" FROM:<${command.from}>")
            output.writeStringUtf8(SMTP_LINE_ENDING)
        }
    }
}

public data class RecipientCommand(val to: String /* params */) : SmtpCommand {
    public object Serializer {
        public suspend fun serialize(command: RecipientCommand, output: AsyncWriter) {
            output.writeStringUtf8("RCPT")
            output.writeStringUtf8(" TO:<${command.to}>")
            output.writeStringUtf8(SMTP_LINE_ENDING)
        }
    }
}

public object DataCommand : SmtpCommand {
    public object Serializer {
        public suspend fun serialize(output: AsyncWriter) {
            output.writeStringUtf8("DATA")
            output.writeStringUtf8(SMTP_LINE_ENDING)
        }
    }
}

public data class MessageCommand(val message: String) : SmtpCommand {
    public object Serializer {
        public suspend fun serialize(command: MessageCommand, output: AsyncWriter) {
            output.writeStringUtf8(command.message)
            output.write('.'.code.toByte())
            output.writeStringUtf8(SMTP_LINE_ENDING)
        }
    }
}

public object QuitCommand : SmtpCommand {
    public object Serializer {
        public suspend fun serialize(output: AsyncWriter) {
            output.writeStringUtf8("QUIT")
            output.writeStringUtf8(SMTP_LINE_ENDING)
        }
    }
}