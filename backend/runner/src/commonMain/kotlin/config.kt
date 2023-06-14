package dev.sitar.kmail.runner

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.file.TomlFileReader
import com.akuleshov7.ktoml.source.TomlSourceReader
import dev.sitar.kmail.agents.smtp.transfer.Proxy
import dev.sitar.kmail.agents.smtp.transports.SMTP_SUBMISSION_PORT
import dev.sitar.kmail.agents.smtp.transports.SMTP_TRANSFER_PORT
import dev.sitar.kmail.smtp.Domain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File

@Serializer(forClass = Domain::class)
object DomainSerializer: KSerializer<Domain> {
    override val descriptor: SerialDescriptor
        get() = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): Domain {
        return Domain.fromText(decoder.decodeString())!!
    }

    override fun serialize(encoder: Encoder, value: Domain) {
        encoder.encodeString(value.asString())
    }
}

@Serializable
data class KmailConfig(
    val domains: List<@Serializable(with = DomainSerializer::class) Domain>,
//    val accounts: List<Account>, // TODO: maybe dont hardcode accounts in a config
    val proxy: Proxy? = null,
//    val storage: Storage,
    val security: Security,
    val smtp: Smtp,
    val imap: Imap,
    val pop3: Pop3,
) {
    // TODO: fix ktoml?
    val storage: Storage = Storage.FileSystemStorage(dir = "mail")

//    TODO: https://github.com/akuleshov7/ktoml/issues/99
//    @Serializable
//    data class Security(
//        val certificates: List<CertificateAndKey>,
//    ) {
//        @Serializable
//        data class CertificateAndKey(val certificate: String, val key: String)
//    }
    // TODO: wait for issue99 to be fixed
    val accounts = listOf(Account("catlover69", "password1234", "marco@localhost"))

    @Serializable
    data class Account(
        val username: String,
        val password: String, // TODO: lol plaintext password
        val email: String
    )

    @Serializable
    data class Proxy(
        val ip: String,
        val port: Int
    ) {
        // TODO: unify this
        fun intoSmtpProxy(): dev.sitar.kmail.agents.smtp.transfer.Proxy {
            return dev.sitar.kmail.agents.smtp.transfer.Proxy(ip, port)
        }
    }

    @Serializable
    data class Security(
        val certificatePaths: List<String>,
        val certificateKeys: List<String>
    )

    @Serializable
    data class Smtp(
        val submission: Submission,
        val transfer: Transfer
    ) {
        @Serializable
        data class Submission(val enabled: Boolean, val port: Int = SMTP_SUBMISSION_PORT)
        @Serializable
        data class Transfer(val enabled: Boolean, val encryption: Boolean = true, val port: Int = SMTP_TRANSFER_PORT)
    }

    @Serializable
    data class Imap(
        val enabled: Boolean
    )

    @Serializable
    data class Pop3(
        val enabled: Boolean
    )

    @Serializable
    sealed interface Storage {
        @Serializable
        @SerialName("FileSystem")
        data class FileSystemStorage(val dir: String): Storage

        @Serializable
        @SerialName("InMemory")
        object InMemoryStorage: Storage
    }
}

val Config: KmailConfig = TomlSourceReader(TomlInputConfig(ignoreUnknownNames = true)).decodeFromString(KmailConfig.serializer(), File("kmail.toml").readLines())