package dev.sitar.kmail.smtp.io

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kio.async.readers.readFully
import dev.sitar.kio.async.writers.AsyncWriter
import dev.sitar.kio.async.writers.writeBytes
import dev.sitar.kio.buffers.Buffer
import dev.sitar.kio.buffers.buffer

internal suspend fun AsyncWriter.writeStringUtf8(string: String) {
    writeBytes(string.toByteArray())
}

internal suspend fun AsyncReader.readStringUtf8(n: Int): String {
    return readFully(n).backingArray!!.decodeToString()
}

internal suspend inline fun AsyncReader.readUntil(hint: Int = 8, until: (Byte) -> Boolean): Buffer {
    return buffer(hint) {
        for (byte in this@readUntil) {
            if (until(byte)) break
            write(byte)
        }
    }
}

internal suspend fun AsyncReader.readUtf8StringUntil(until: (Char) -> Boolean): String {
    return readUntil { until(it.toInt().toChar()) }.toByteArray().decodeToString()
}