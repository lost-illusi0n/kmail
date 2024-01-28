package dev.sitar.kmail.agents.smtp.transports.server

import dev.sitar.keystone.Pipeline
import dev.sitar.keystone.Stage
import dev.sitar.kmail.smtp.SmtpCommand
import kotlin.Exception

data class SmtpCommandContext(val command: SmtpCommand, var wasProcessed: Boolean)

class SmtpCommandPipeline: Pipeline<SmtpCommandContext>(setOf(Logging, Before, Processing, After), onFilter = { !it.wasProcessed }) {
    companion object {
        val Logging = Stage("Logging")
        val Before = Stage("Before")
        val Processing = Stage("Processing")
        val After = Stage("After")
    }

    operator fun invoke(block: SmtpCommandPipeline.() -> Unit) {
        block(this)
    }
}