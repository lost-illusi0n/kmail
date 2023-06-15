package dev.sitar.kmail.agents.smtp

import dev.sitar.kmail.smtp.frames.replies.SmtpReply


internal sealed interface StepProgression {
    object Continue: StepProgression
    object Retry: StepProgression
    class Abort(val reason: String): StepProgression
}

internal fun SmtpReply<*>.coerceToStepProgression(): StepProgression {
    return when (this) {
        is SmtpReply.PositiveCompletion, is SmtpReply.PositiveIntermediate -> StepProgression.Continue
        is SmtpReply.TransientNegative -> StepProgression.Retry
        is SmtpReply.PermanentNegative -> StepProgression.Abort("Received a permanent negative response! $this")
    }
}

internal typealias Step = suspend () -> StepProgression

internal typealias Stop = suspend (StopReason) -> Unit

internal sealed interface StopReason {
    object Normal : StopReason
    class Abrupt(val reason: String) : StopReason
}

internal class Machine(val steps: List<Step>, val stop: Stop?) {
    suspend fun run() {
        if (steps.isEmpty()) return

        val stepsIterator = steps.iterator()
        var step = stepsIterator.next()

        var stopReason: StopReason = StopReason.Normal

        while (true) {
            when (val result = step()) {
                StepProgression.Continue -> {
                    if (!stepsIterator.hasNext()) break

                    step = stepsIterator.next()
                }

                StepProgression.Retry -> {
                    continue
                }

                is StepProgression.Abort -> {
                    stopReason = StopReason.Abrupt(result.reason)
                    break

                }
            }
        }

        stop?.let { it(stopReason) }
    }

    class Builder {
        val steps: MutableList<Step> = mutableListOf()
        var stop: Stop? = null

        fun step(step: Step) {
            steps += step
        }

        fun stop(stop: Stop) {
            this.stop = stop
        }

        fun build(): Machine {
            return Machine(steps, stop)
        }
    }
}

internal suspend fun machine(machine: Machine.Builder.() -> Unit) {
    return Machine.Builder().apply(machine).build().run()
}