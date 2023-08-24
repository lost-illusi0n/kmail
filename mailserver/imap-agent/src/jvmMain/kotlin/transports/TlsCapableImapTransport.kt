package dev.sitar.kmail.imap.agent.transports

import dev.sitar.kio.async.readers.toAsyncReader
import dev.sitar.kmail.imap.agent.io.asImapServerReader
import dev.sitar.kmail.imap.agent.io.asImapServerWriter
import dev.sitar.kmail.imap.frames.command.TaggedImapCommand
import dev.sitar.kmail.imap.frames.response.TaggedImapResponse
import dev.sitar.kmail.utils.io.toAsyncWriterStream
import io.ktor.util.network.*
import mu.KotlinLogging
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket

private val logger = KotlinLogging.logger { }

