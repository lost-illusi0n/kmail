package dev.sitar.kmail.pop3.commands

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

interface Pop3CommandDeserializer<T: Pop3Command> {
    suspend fun deserialize(reader: AsyncReader): T
}

class NoneArgumentPop3CommandDeserializer<T: Pop3Command>(private val instance: T): Pop3CommandDeserializer<T> {
    override suspend fun deserialize(reader: AsyncReader): T {
        reader.readUtf8UntilLineEnd()
        return instance
    }
}