package dev.sitar.kmail.smtp

import dev.sitar.kmail.smtp.io.AsyncSmtpReader

public sealed interface SmtpReply

public typealias EhloKeyword = String
public typealias EhloParam = String

public data class GreetReply(val data: String) : SmtpReply {
    public object Serializer {
        public suspend fun deserialize(input: AsyncSmtpReader): GreetReply {
            require(input.readIsFinal())

            return GreetReply(input.readUtf8UntilSmtpEnding())
        }
    }
}

public data class EhloReply(val domain: String, val greet: String?, val lines: Map<EhloKeyword, EhloParam?>) : SmtpReply {
    public object Serializer {
        public suspend fun deserialize(input: AsyncSmtpReader): EhloReply {
            // the first character (space or -) after response code tells us whether this is the last line of the reply
            var isFinal = input.readIsFinal()

            val domainOrGreet = input.readUtf8UntilSmtpEnding().split(' ')
            val (domain, greet) = when (domainOrGreet.size) {
                1 -> Pair(domainOrGreet[0], null)
                2 -> Pair(domainOrGreet[0], domainOrGreet[1])
                else -> error("cannot parse ehlo reply")
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
        public suspend fun deserialize(input: AsyncSmtpReader): OkReply {
            return OkReply(input.readUtf8UntilSmtpEnding())
        }
    }
}

public object StartMailInputReply: SmtpReply {
    public object Serializer {
        public suspend fun deserialize(input: AsyncSmtpReader): StartMailInputReply {
            input.readUtf8UntilSmtpEnding()
            return StartMailInputReply
        }
    }
}