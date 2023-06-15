package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.Domain

public const val STARTTLS: String = "STARTTLS"

public data class EhloCompletion(
    val domain: Domain,
    val greet: String?,
    val capabilities: List<String>
) :
    SmtpReply.PositiveCompletion by SmtpReply.PositiveCompletion.Default(
        code = 250,
        lines = listOf("${domain.asString()}${greet?.let { " $it" }}") + capabilities
    ) {
    public companion object {
        public fun from(lines: List<String>): EhloCompletion {
            lateinit var domain: String
            var greet: String? = null
            var capabilities: List<String> = emptyList()

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
                        capabilities = lines.drop(1)
                    }
                }
            }

            return EhloCompletion(Domain.fromText(domain)!!, greet, capabilities)
        }
    }
}