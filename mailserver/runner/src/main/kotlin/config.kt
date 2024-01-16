package dev.sitar.kmail.runner

import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import com.fasterxml.jackson.annotation.JsonIgnore
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
import dev.sitar.kmail.imap.agent.ImapConfig
import dev.sitar.kmail.runner.storage.filesystems.LocalFileSystem
import dev.sitar.kmail.smtp.Domain
import java.io.File

private object DomainSerializer : StdDeserializer<Domain>(Domain::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Domain {
        return Domain.fromText(p.valueAsString)!!
    }
}

private object BucketLocationSerializer : StdDeserializer<BucketLocationConstraint>(BucketLocationSerializer::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BucketLocationConstraint {
        return BucketLocationConstraint.fromValue(p.valueAsString)
    }
}

//@Serializable
data class KmailConfig(
    val domains: List<Domain>,
    @JsonProperty("accounts")
    val accountStorage: Accounts = Accounts("", Accounts.Hash.Argon2),
    val proxy: Proxy? = null,
    val mailbox: Mailbox,
    val security: Security,
    val smtp: Smtp,
    val imap: Imap,
    val pop3: Pop3,
) {
    @JsonIgnore
    val accounts = retrieveAccounts()

    private fun retrieveAccounts(): List<Account> {
        val contents = LocalFileSystem(accountStorage.dir).readFile("accounts.kmail")?.decodeToString()
        require(contents != null) { "accounts file is missing. it is not auto-generated yet."}

        return contents.lines().map { entry ->
            val (email, hash) = entry.split(' ')
            Account(email, hash)
        }
    }

    data class Accounts(
        val dir: String = "",
        val hash: Hash,
        val format: Format = Format.Text
    ) {
        enum class Hash {
            @JsonProperty("none")
            None,
            @JsonProperty("argon2")
            Argon2
        }

        enum class Format {
            @JsonProperty("text")
            Text
//            @JsonProperty("sqlite")
//            SQLite
        }
    }

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

            @JsonTypeName("in-memory")
            object InMemory: Filesystem

            @JsonTypeName("s3")
            data class S3(val region: BucketLocationConstraint, val bucket: Bucket, @JsonProperty("root-file") val rootFolder: String?, val credentials: Credentials?, val backend: Backend?): Filesystem {
                data class Bucket(val name: String, val location: BucketLocationConstraint)
                data class Backend(val endpoint: String, val pathStyleAccessEnabled: Boolean = false)
                data class Credentials(val username: String, val password: String)
            }
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

    data class Security(val keystore: String, val password: String)
    data class Smtp(
        val submission: Submission,
        val transfer: Transfer
    ) {
        data class Submission(val enabled: Boolean, val port: Int = SMTP_SUBMISSION_PORT)
        data class Transfer(val enabled: Boolean, val encryption: Boolean = true, val port: Int = SMTP_TRANSFER_PORT)
    }

    data class Imap(
        val enabled: Boolean,
        val allowInsecurePassword: Boolean = false
    ) {
        fun toImapConfig() = ImapConfig(allowInsecurePassword)
    }

    data class Pop3(
        val enabled: Boolean
    )
}

val Config: KmailConfig = TomlMapper().registerKotlinModule().registerModule(
    SimpleModule()
        .addDeserializer(Domain::class.java, DomainSerializer)
        .addDeserializer(BucketLocationConstraint::class.java, BucketLocationSerializer)
).readValue(File("kmail.toml"), KmailConfig::class.java)