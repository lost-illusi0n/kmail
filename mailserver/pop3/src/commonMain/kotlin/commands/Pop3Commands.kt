package dev.sitar.kmail.pop3.commands

import dev.sitar.kio.async.readers.AsyncReader
import dev.sitar.kmail.utils.io.readUtf8UntilLineEnd

object QuitCommand : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<QuitCommand> by NoneArgumentPop3CommandDeserializer(QuitCommand)
}

object StatCommand : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<StatCommand> by NoneArgumentPop3CommandDeserializer(StatCommand)
}

data class ListCommand(val messageNumber: Int?) : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<ListCommand> {
        override suspend fun deserialize(reader: AsyncReader): ListCommand {
            val line = reader.readUtf8UntilLineEnd()

            if (line == "\r\n") return ListCommand(null)

            return ListCommand(line.drop(1).toIntOrNull())
        }
    }
}

data class RetrCommand(val messageNumber: Int) : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<RetrCommand> {
        override suspend fun deserialize(reader: AsyncReader): RetrCommand {
            reader.read()
            return RetrCommand(reader.readUtf8UntilLineEnd().toInt())
        }
    }
}

data class DeleCommand(val messageNumber: Int) : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<RetrCommand> {
        override suspend fun deserialize(reader: AsyncReader): RetrCommand {
            reader.read()
            return RetrCommand(reader.readUtf8UntilLineEnd().toInt())
        }
    }
}

object NoopCommand : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<NoopCommand> by NoneArgumentPop3CommandDeserializer(NoopCommand)
}

object RsetCommand : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<RsetCommand> by NoneArgumentPop3CommandDeserializer(RsetCommand)
}

data class UserCommand(val name: String) : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<UserCommand> {
        override suspend fun deserialize(reader: AsyncReader): UserCommand {
            reader.read()
            return UserCommand(reader.readUtf8UntilLineEnd())
        }
    }
}

data class PassCommand(val password: String) : Pop3Command {
    object Deserializer : Pop3CommandDeserializer<PassCommand> {
        override suspend fun deserialize(reader: AsyncReader): PassCommand {
            reader.read()
            return PassCommand(reader.readUtf8UntilLineEnd())
        }
    }
}
