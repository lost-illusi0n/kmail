package dev.sitar.kmail.smtp.io.smtp.reader

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.smtp.*
import dev.sitar.kmail.smtp.io.readStringUtf8

public class AsyncSmtpServerReader(reader: AsyncReader) : AsyncSmtpReader, AsyncReader by reader {
    public suspend fun readSmtpCommandKey(): String {
        return readStringUtf8(4)
    }

    // TODO: instead of always expecting T (a good result), take a map (status code -> expected reply)
    public suspend inline fun <reified T : SmtpCommand> readSmtpCommand(): T {
        // a message is not a real command. take care of it special
        if (T::class == MessageCommand::class) return MessageCommand.Serializer.deserialize(this) as T

        return when (val command = readSmtpCommandKey()) {
            "EHLO" -> EhloCommand.Serializer.deserialize(this) as T
            "MAIL" -> MailCommand.Serializer.deserialize(this) as T
            "RCPT" -> RecipientCommand.Serializer.deserialize(this) as T
            "DATA" -> DataCommand.Serializer.deserialize(this) as T
            "QUIT" -> QuitCommand.Serializer.deserialize(this) as T
            else -> error("unknown command: $command")
        }
    }
}

public fun AsyncReader.asAsyncSmtpServerReader(): AsyncSmtpServerReader {
    return AsyncSmtpServerReader(this)
}