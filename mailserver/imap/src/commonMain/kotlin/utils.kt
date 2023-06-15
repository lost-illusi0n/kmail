package dev.sitar.kmail.imap

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8StringUntil
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

internal suspend fun AsyncReader.readValue(isEnd: Boolean): String {
    return (if (isEnd) readUtf8UntilLineEnd() else readUtf8StringUntil { it == ' ' }).removePrefix("\"").removeSuffix("\"")
}

internal suspend fun AsyncReader.readList(): List<String> {
    if (read().toInt().toChar() != '(') TODO("bad syntax")

    val list = readUtf8StringUntil { it == ')' }

    return list.split(' ')
}