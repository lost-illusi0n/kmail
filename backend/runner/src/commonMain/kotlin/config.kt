package dev.sitar.kmail.runner

import com.akuleshov7.ktoml.file.TomlFileReader
import com.akuleshov7.ktoml.source.TomlSourceReader
import dev.sitar.kmail.agents.smtp.transports.SMTP_SUBMISSION_PORT
import dev.sitar.kmail.agents.smtp.transports.SMTP_TRANSFER_PORT
import dev.sitar.kmail.smtp.Domain
import kotlinx.serialization.KSerializer
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
    val security: Security,
    val smtp: Smtp,
    val imap: Imap,
    val pop3: Pop3
) {
//    TODO: https://github.com/akuleshov7/ktoml/issues/99
//    @Serializable
//    data class Security(
//        val certificates: List<CertificateAndKey>,
//    ) {
//        @Serializable
//        data class CertificateAndKey(val certificate: String, val key: String)
//    }
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
        data class Transfer(val enabled: Boolean, val port: Int = SMTP_TRANSFER_PORT)
    }

    @Serializable
    data class Imap(
        val enabled: Boolean
    )

    @Serializable
    data class Pop3(
        val enabled: Boolean
    )
}

// TODO: read this from filesystem
val Config: KmailConfig = TomlSourceReader.decodeFromString(KmailConfig.serializer(), File("kmail.toml").readLines())

//    KmailConfig(Domain.fromText("[0.0.0.0]")!!, "changeme")