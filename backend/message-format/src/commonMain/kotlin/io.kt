package dev.sitar.kmail.message

import dev.sitar.kio.buffers.Buffer
import dev.sitar.kio.buffers.SequentialReader
import dev.sitar.kio.buffers.buffer
import dev.sitar.kio.buffers.readBytes

internal fun SequentialReader.readStringUtf8(n: Int): String {
    return readBytes(n).backingArray!!.decodeToString()
}

internal fun SequentialReader.peek(): Byte {
    val byte = read()
    readIndex--
    return byte
}

internal inline fun SequentialReader.readUntil(hint: Int = 8, until: (Byte) -> Boolean): Buffer {
    return buffer(hint) {
        for (byte in this@readUntil) {
            if (until(byte)) break
            write(byte)
        }
    }
}

internal  fun SequentialReader.readUtf8StringUntil(until: (Char) -> Boolean): String {
    return readUntil { until(it.toInt().toChar()) }.toByteArray().decodeToString()
}

internal fun SequentialReader.readUtf8UntilMailEnding(): String {
    var lastChar = ' '

    return readUtf8StringUntil {
        val isEnding = lastChar == '\r' && it == '\n'
        lastChar = it
        isEnding
    }.dropLast(1)
}