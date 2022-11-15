package dev.sitar.kmail.message.headers

public fun subject(subject: String): Header {
    return Header("Subject", subject)
}
