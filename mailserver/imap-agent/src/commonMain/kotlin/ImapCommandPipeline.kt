package dev.sitar.kmail.imap.agent

import dev.sitar.keystone.Pipeline
import dev.sitar.keystone.Stage
import dev.sitar.kmail.imap.frames.command.ImapCommand
import dev.sitar.kmail.imap.frames.command.TaggedImapCommand

data class ImapCommandContext(val command: TaggedImapCommand, var wasProcessed: Boolean)

class ImapCommandPipeline: Pipeline<ImapCommandContext>(Logging, Before, Processing, After) {
    companion object {
        val Logging = Stage("Logging")
        val Before = Stage("Before")
        val Processing = Stage("Processing")
        val After = Stage("After")
    }

    operator fun invoke(block: ImapCommandPipeline.() -> Unit) {
        block(this)
    }
}