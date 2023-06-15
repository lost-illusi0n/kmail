package dev.sitar.kmail.agents.pop3

import dev.sitar.keystone.Pipeline
import dev.sitar.keystone.Stage
import dev.sitar.kmail.pop3.commands.Pop3Command

data class Pop3CommandContext(val command: Pop3Command, var wasProcessed: Boolean)

class Pop3CommandPipeline: Pipeline<Pop3CommandContext>(setOf(Logging, Before, Processing, After)) {
    companion object {
        val Logging = Stage("Logging")
        val Before = Stage("Before")
        val Processing = Stage("Processing")
        val After = Stage("After")
    }

    operator fun invoke(block: Pop3CommandPipeline.() -> Unit) {
        block(this)
    }
}