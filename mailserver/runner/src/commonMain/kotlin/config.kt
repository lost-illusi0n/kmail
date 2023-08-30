package dev.sitar.kmail.runner

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.sitar.kmail.agents.smtp.transports.SMTP_SUBMISSION_PORT
import dev.sitar.kmail.agents.smtp.transports.SMTP_TRANSFER_PORT
import dev.sitar.kmail.smtp.Domain
import java.io.File

object DomainSerializer : StdDeserializer<Domain>(Domain::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Domain {
        return Domain.fromText(p.valueAsString)!!
    }
}

//@Serializable
data class KmailConfig(
    val domains: List<Domain>,
//    val accounts: List<Account>, // TODO: maybe dont hardcode accounts in a config
    val proxy: Proxy? = null,
//    val storage: Storage,
    val mailbox: Mailbox,
    val security: Security,
    val smtp: Smtp,
    val imap: Imap,
    val pop3: Pop3,
) {
    // TODO: wait for issue99 to be fixed
    val accounts = listOf(Account("catlover69", "password1234", "marco@storm.sitar.dev"))

    data class Account(
        val username: String,
        val password: String, // TODO: lol plaintext password
        val email: String
    )

    data class Mailbox(
        val format: Format,
        val filesystem: Filesystem
    ) {
        enum class Format {
            @JsonProperty("maildir")
            Maildir
        }

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
        sealed interface Filesystem {
            @JsonTypeName("local")
            data class Local(val dir: String): Filesystem
        }
    }

    data class Proxy(
        val ip: String,
        val port: Int
    ) {
        // TODO: unify this
        fun intoSmtpProxy(): dev.sitar.kmail.agents.smtp.transfer.Proxy {
            return dev.sitar.kmail.agents.smtp.transfer.Proxy(ip, port)
        }
    }

    data class Security(val certificates: List<CertificateAndKey>) {
        data class CertificateAndKey(val certificate: String, val key: String)
    }
    data class Smtp(
        val submission: Submission,
        val transfer: Transfer
    ) {
        data class Submission(val enabled: Boolean, val port: Int = SMTP_SUBMISSION_PORT)
        data class Transfer(val enabled: Boolean, val encryption: Boolean = true, val port: Int = SMTP_TRANSFER_PORT)
    }

    data class Imap(
        val enabled: Boolean
    )

    data class Pop3(
        val enabled: Boolean
    )
}

val Config: KmailConfig = TomlMapper().registerKotlinModule().registerModule(SimpleModule().addDeserializer(Domain::class.java, DomainSerializer)).readValue(File("kmail.toml"), KmailConfig::class.java)