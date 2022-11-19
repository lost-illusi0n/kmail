package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.io.smtp.writer.AsyncSmtpServerWriter
import dev.sitar.kmail.smtp.io.writeStringUtf8

public typealias EhloKeyword = String

public typealias EhloParam = String?

public data class EhloCompletion(
    val domain: String,
    val greet: String?,
    val capabilities: Map<EhloKeyword, EhloParam>
) :
    SmtpReply.PositiveCompletion by SmtpReply.PositiveCompletion.Default(
        code = 250,
        data = "$domain${greet?.let { " $it" }}",
        lines = capabilities.map { (key, param) -> "$key${param?.let { " $it" }}" }
    ) {
    public companion object {
        public fun from(data: String, lines: List<String>): EhloCompletion {
            val (domain, greet) = when (val index = data.indexOf(' ')) {
                -1 -> Pair(data, null)
                else -> Pair(data.substring(0..index), data.substring(index))
            }


            val capabilities = lines.associate {
                when (val index = it.indexOf(' ')) {
                    -1 -> Pair(it, null)
                    else -> Pair(it.substring(0..index), it.substring(index))
                }
            }

            return EhloCompletion(domain, greet, capabilities)
        }

        public suspend fun serialize(output: AsyncSmtpServerWriter, ehlo: EhloCompletion) {
            output.writeIsFinal(ehlo.lines.isEmpty())

            output.writeStringUtf8(ehlo.domain)

            if (ehlo.greet != null) output.writeStringUtf8(" ${ehlo.greet}")

            output.endLine()

            ehlo.capabilities.onEachIndexed { i, (keyword, param) ->
                output.writeIsFinal(i == ehlo.lines.size)

                output.writeStringUtf8(keyword)

                if (param != null) output.writeStringUtf8(" $param")

                output.endLine()
            }
        }
    }
}