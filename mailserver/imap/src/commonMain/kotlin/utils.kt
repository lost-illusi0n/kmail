package dev.sitar.kmail.imap

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8StringUntil
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd
import dev.sitar.kmail.utils.todo

internal suspend fun AsyncReader.readValue(isEnd: Boolean): String {
    return (if (isEnd) readUtf8UntilLineEnd() else readUtf8StringUntil { it == ' ' }).removePrefix("\"").removeSuffix("\"")
}

internal suspend fun AsyncReader.readList(checkFirst: Boolean = true): List<String> {
    if (checkFirst && read().toInt().toChar() != '(') todo("bad syntax")

    val list = readUtf8StringUntil { it == ')' }

    return list.split(' ')
}