package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.Domain

public typealias EhloKeyword = String

public typealias EhloParam = String?

public const val STARTTLS: EhloKeyword = "STARTTLS"

public data class EhloCompletion(
    val domain: Domain,
    val greet: String?,
    val capabilities: Map<EhloKeyword, EhloParam>
) :
    SmtpReply.PositiveCompletion by SmtpReply.PositiveCompletion.Default(
        code = 250,
        lines = listOf("${domain.asString()}${greet?.let { " $it" }}") + capabilities.map { (key, param) -> "$key${param?.let { " $it" } ?: ""}" }
    ) {
    public companion object {
        public fun from(lines: List<String>): EhloCompletion {
            lateinit var domain: String
            var greet: String? = null
            var capabilities: Map<EhloKeyword, EhloParam> = emptyMap()

            for ((index, line) in lines.withIndex()) {
                when (index) {
                    0 -> {
                        when (val index = line.indexOf(' ')) {
                            -1 -> domain = line
                            else -> {
                                domain = line.substring(0..index)
                                greet = line.substring(index)
                            }
                        }
                    }

                    else -> {
                        capabilities = lines.asSequence().drop(1).associate {
                            when (val index = it.indexOf(' ')) {
                                -1 -> it to null
                                else -> it.substring(0..index) to it.substring(index)
                            }
                        }
                    }
                }
            }

            return EhloCompletion(Domain.fromText(domain)!!, greet, capabilities)
        }
    }
}