package dev.sitar.kmail.smtp

import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpClientReader
import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpReader
import dev.sitar.kmail.smtp.io.writeStringUtf8

public sealed interface SmtpReply

public typealias EhloKeyword = String
public typealias EhloParam = String

public data class GreetReply(val greet: String) : SmtpReply {
    public object Serializer {
        public suspend fun serialize(output: AsyncSmtpServerWriter, greetReply: GreetReply) {
            output.writeIsFinal(true)
            output.writeStringUtf8(greetReply.greet)
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): GreetReply {
            require(input.readIsFinal())

            return GreetReply(input.readUtf8UntilSmtpEnding())
        }
    }
}

public data class EhloReply(val domain: String, val greet: String?, val lines: Map<EhloKeyword, EhloParam?>) : SmtpReply {
    public object Serializer {
        public suspend fun serialize(output: AsyncSmtpServerWriter, ehlo: EhloReply) {
            output.writeIsFinal(ehlo.lines.isEmpty())

            output.writeStringUtf8(ehlo.domain)

            if (ehlo.greet != null) output.writeStringUtf8(" ${ehlo.greet}")

            output.endLine()

            ehlo.lines.onEachIndexed { i, (keyword, param) ->
                output.writeIsFinal(i == ehlo.lines.size)

                output.writeStringUtf8(keyword)

                if (param != null) output.writeStringUtf8(" $param")

                output.endLine()
            }
        }

        public suspend fun deserialize(input: AsyncSmtpClientReader): EhloReply {
            var isFinal = input.readIsFinal()

            val domainOrGreet = input.readUtf8UntilSmtpEnding()
            val (domain, greet) = when (val index = domainOrGreet.indexOf(' ')) {
                -1 -> Pair(domainOrGreet, null)
                else -> Pair(domainOrGreet.substring(0..index), domainOrGreet.substring(index))
            }

            val lines = mutableMapOf<EhloKeyword, EhloParam?>()

            // read the rest of the reply
            while (!isFinal) {
                require(input.readSmtpCode() == 250)

                isFinal = input.readIsFinal()

                val keywordAndParam = input.readUtf8UntilSmtpEnding().split(' ')
                val (keyword, param) = when (keywordAndParam.size) {
                    1 -> Pair(keywordAndParam[0], null)
                    2 -> Pair(keywordAndParam[0], keywordAndParam[1])
                    else -> error("cannot parse ehlo reply")
                }

                lines[keyword] = param
            }

            return EhloReply(domain, greet, lines)
        }
    }
}

public data class OkReply(val data: String): SmtpReply {
    public object Serializer {
        public suspend fun serialize(output: AsyncSmtpServerWriter, ok: OkReply) {
            output.writeIsFinal(true)
            output.writeStringUtf8(ok.data)
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): OkReply {
            return OkReply(input.readUtf8UntilSmtpEnding())
        }
    }
}

public object StartMailInputReply: SmtpReply {
    public object Serializer {
        public suspend fun serialize(output: AsyncSmtpServerWriter) {
            output.writeIsFinal(true)
            output.endLine()
        }

        public suspend fun deserialize(input: AsyncSmtpReader): StartMailInputReply {
            input.readUtf8UntilSmtpEnding()
            return StartMailInputReply
        }
    }

    override fun toString(): String {
        return "StartMailInputReply"
    }
}