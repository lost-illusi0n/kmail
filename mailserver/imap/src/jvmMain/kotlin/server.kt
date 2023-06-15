//package dev.sitar.kmail.imap
//
//import dev.sitar.kio.async.readers.AsyncReader
//import dev.sitar.kio.async.writers.toAsyncWriter
//import dev.sitar.kio.buffers.DefaultBufferPool
//import dev.sitar.kio.use
//import dev.sitar.kmail.imap.frames.Tag
//import dev.sitar.kmail.imap.frames.command.*
//import dev.sitar.kmail.imap.frames.response.*
//import dev.sitar.kmail.utils.io.*
//import io.ktor.network.selector.*
//import io.ktor.network.sockets.*
//import io.ktor.util.*
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.coroutineScope
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//
//enum class State {
//    NotAuthenticated,
//    Authenticated,
//    Selected,
//    Logout
//}
//
//class ImapServerReader(val reader: AsyncReader) : AsyncReader by reader {
//    private suspend fun readCommandIdentifier(): String {
//        var lastChar = '\u0000'
//
//        val tag = readUtf8StringUntil {
//            val t = it == ' ' || (lastChar == '\r' && it == '\n')
//            lastChar = it
//            t
//        }
//
//        return if (lastChar == '\n') tag.dropLast(1) else tag
//    }
//
//    suspend fun readCommand(): TaggedImapCommand? {
//        val tag = Tag.deserialize(this)
//
//        val identRaw = readCommandIdentifier()
//        val identifier = ImapCommand.Identifier.findByIdentifier(identRaw) ?: TODO("unknown command: $identRaw")
//
//        return TaggedImapCommand(tag, identifier.serializer.deserialize(this))
//    }
//}
//
//fun AsyncReader.asImapServerReader(): ImapServerReader {
//    return ImapServerReader(this)
//}
//
//class ImapServerWriter(val writer: AsyncWriterStream) : AsyncWriterStream by writer {
//    suspend fun writeResponse(taggedResponse: TaggedImapResponse) {
//        taggedResponse.tag.serialize(this)
//        taggedResponse.response.serialize(this)
//        writeLineEnd()
//
//        flush()
//    }
//}
//
//fun AsyncWriterStream.asImapServerWriter(): ImapServerWriter {
//    return ImapServerWriter(this)
//}
//
//suspend fun main() = coroutineScope {
//    val serverSocket = aSocket(SelectorManager(Dispatchers.Default)).tcp().bind(port = 143)
//    val socket = serverSocket.accept()
//    println("accepted connection")
//    val reader = socket.openReadChannel().toAsyncReader().asImapServerReader()
//    val writer = socket.openWriteChannel().toAsyncWriterStream().asImapServerWriter()
//
//    val commands = Channel<TaggedImapCommand>()
//
//    launch {
//        while (!socket.isClosed) {
//            val command = reader.readCommand()!!
//            println("IMAP <<< $command")
//            commands.send(command)
//        }
//    }
//
//    suspend fun defaultHandler(taggedCommand: TaggedImapCommand) {
//        when (taggedCommand.command) {
//            CapabilityCommand -> {
//                writer.writeResponse(Tag.Untagged, CapabilityResponse(listOf(Capabilities.Imap4Rev1)))
//                writer.writeResponse(taggedCommand.tag, OkResponse(text = "CAPABILITY completed."))
//            }
//
//            NoOpCommand -> {
//                writer.writeResponse(taggedCommand.tag, OkResponse(text = "NOOP completed."))
//            }
//            LogoutCommand -> TODO()
//            else -> error("not possible command: $taggedCommand")
//        }
//    }
//
////    writer.writeResponse(Tag.Untagged, OkResponse(text = "Greetings from Kmail."))
//    writer.writeResponse(Tag.Untagged, PreAuthResponse(text = "Authenticated greetings from Kmail."))
//
////    var state = State.NotAuthenticated
//    var state = State.Authenticated
//
//    while (true) {
//        when (state) {
//            State.NotAuthenticated -> {
//                val taggedCommand = commands.receive()
//
//                when (taggedCommand.command) {
//                    StartTlsCommand -> {
//                        writer.writeResponse(taggedCommand.tag, OkResponse(text = "Let the TLS negotiations begin."))
//                        TODO("TLS")
//                    }
//                    // TODO: authenticate and login
//                    else -> defaultHandler(taggedCommand)
//                }
//            }
//
//            State.Authenticated -> {
//                val taggedCommand = commands.receive()
//
//                when (taggedCommand.command) {
//                    is SelectCommand -> {
//                        writer.writeResponse(Tag.Untagged, FlagsResponse(flags = FlagsResponse.SYSTEM_FLAGS + "Sent"))
//                        writer.writeResponse(Tag.Untagged, ExistsResponse(n = 1))
//                        writer.writeResponse(Tag.Untagged, RecentResponse(n = 1))
//                        writer.writeResponse(Tag.Untagged, OkResponse(text = "[UIDVALIDITY 1]"))
//                        writer.writeResponse(Tag.Untagged, OkResponse(text = "[UIDNEXT 1]"))
//                        writer.writeResponse(taggedCommand.tag, OkResponse(text = "SELECT complete."))
//
//                        state = State.Selected
//                    }
//
//                    else -> defaultHandler(taggedCommand)
//                }
//            }
//
//            State.Selected -> {
//                val taggedCommand = commands.receive()
//
//                when (taggedCommand.command) {
//                    is UidCommand -> {
//                        // TODO: access mail
//                        writer.writeResponse(taggedCommand.tag, OkResponse(text = "FETCH completed."))
//                    }
//                    else -> defaultHandler(taggedCommand)
//                }
//
//            }
//            State.Logout -> TODO()
//        }
//    }
//}