package dev.sitar.kmail.message

import dev.sitar.kmail.message.headers.*
import kotlinx.datetime.Clock
import kotlinx.datetime.UtcOffset

fun generateMessageId(): String {
    return "<OPAKSDPOKD@POKASDPOKASPOD>"
}

val message = message {
    headers {
        +messageId(generateMessageId())
        +originalDate(Clock.System.now(), UtcOffset(-5))
        +toRcpt("<admin@example.com>")
        +from("Foo <foo@bar.com>")
        +subject("an important subject")
    }

    body {
        line("important!")
    }
}

fun main() {
    val message = message {
        headers {
            +messageId(generateMessageId())
            +originalDate(Clock.System.now(), UtcOffset(-5))
            +toRcpt("<admin@example.com>")
            +from("Foo <foo@bar.com>")
            +subject("an important subject")
        }

        body {
            line("important!")
        }
    }.asText()
}