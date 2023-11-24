package dev.sitar.kmail.runner

import aws.smithy.kotlin.runtime.util.decodeBase64Bytes
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

fun passVerify(password: String, hash: String): Boolean {
    return when (Config.accountStorage.hash) {
        KmailConfig.Accounts.Hash.None -> password.contentEquals(hash)
        KmailConfig.Accounts.Hash.Argon2 -> argon2Verify(password, hash)
    }
}

fun argon2Verify(password: String, hash: String): Boolean {
    val parts = hash.split('$').drop(1)
    require(parts.size >= 5)

    val variant = when(parts[0]) {
        "argon2d" -> Argon2Parameters.ARGON2_d
        "argon2i" -> Argon2Parameters.ARGON2_i
        "argon2id" -> Argon2Parameters.ARGON2_id
        else -> error("unknown variant: ${parts[0]}")
    }

    require(parts[1].startsWith("v="))
    val version = parts[1].substring(2).toInt()

    val performance = parts[2].split(',')
    require(performance.size == 3)

    require(performance[0].startsWith("m="))
    val mem = performance[0].substring(2).toInt()

    require(performance[1].startsWith("t="))
    val iterations = performance[1].substring(2).toInt()

    require(performance[2].startsWith("p="))
    val parallelism = performance[2].substring(2).toInt()

    val salt = parts[3].padEnd(parts[3].length + parts[3].length.mod(4), '=').decodeBase64Bytes()
    val hash = parts[4].padEnd(parts[4].length + parts[4].length.mod(4), '=').decodeBase64Bytes()

    val newHash = ByteArray(hash.size) { 0 }

    val generator = Argon2BytesGenerator()
    generator.init(Argon2Parameters.Builder(variant).withVersion(version).withMemoryAsKB(mem).withIterations(iterations).withParallelism(parallelism).withSalt(salt).build())
    generator.generateBytes(password.toCharArray(), newHash)

    return newHash.contentEquals(hash)
}