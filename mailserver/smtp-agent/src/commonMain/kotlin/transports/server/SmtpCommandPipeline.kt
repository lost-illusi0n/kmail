package dev.sitar.kmail.agents.smtp.transports.server

import dev.sitar.keystone.Pipeline
import dev.sitar.keystone.Stage
import dev.sitar.kmail.smtp.SmtpCommand
import kotlin.Exception

sealed interface SmtpCommandContext {
    var continuePropagation: Boolean

    data class Known(val command: SmtpCommand, override var continuePropagation: Boolean): SmtpCommandContext
    class Unknown(val cause: Exception, override var continuePropagation: Boolean): SmtpCommandContext
}

class SmtpCommandPipeline: Pipeline<SmtpCommandContext>(setOf(Logging, Global, Process), onFilter = { it.continuePropagation }) {
    companion object {
        val Logging = Stage("Logging")
        val Global = Stage("Global")
        val Process = Stage("Process")
    }

    operator fun invoke(block: SmtpCommandPipeline.() -> Unit) {
        block(this)
    }
}

/**
 * SmtpCommandPipeline {
 *      in(Process) {
 *
 *      }
 * }
 */

//data class SmtpCommandCall(
//    val command: SmtpCommand,
//    var processed: Boolean
//)
//
//class SmtpCommandPipeline : Pipeline<Any, SmtpCommandCall>(Command) {
//    companion object Phase {
//        val Command = PipelinePhase("Command")
//    }
//}
//
//suspend fun a() {
//    val pipeline = SmtpCommandPipeline()
//
//    pipeline.execute(SmtpCommandCall(HeloCommand("asd")), Any())
//}