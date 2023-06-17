package dev.sitar.kmail.pop3.io

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.pop3.commands.Pop3Command
import dev.sitar.kmail.pop3.commands.Pop3CommandName
import dev.sitar.kmail.pop3.commands.UnknownCommand
import dev.sitar.kmail.utils.io.readStringUtf8
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

class AsyncPop3ServerReader(reader: AsyncReader): AsyncReader by reader {
    private suspend fun readCommandName(): String {
        return readStringUtf8(4)

//        var lastChar = '\u0000'
//
//        val tag = readUtf8StringUntil {
//            val t = it == ' ' || (lastChar == '\r' && it == '\n')
//            lastChar = it
//            t
//        }
//
//        return if (lastChar == '\n') tag.dropLast(1) else tag
    }

    suspend fun readCommand(): Pop3Command {
        val raw = readCommandName()
        val name = Pop3CommandName.fromName(raw) ?: return UnknownCommand("$raw|${readUtf8UntilLineEnd()}")

        return name.deserializer.deserialize(this)
    }
}

fun AsyncReader.asPop3ServerReader(): AsyncPop3ServerReader {
    return AsyncPop3ServerReader(this)
}