package dev.sitar.kmail.message.headers

fun subject(subject: String): Header {
    return Header(Headers.Subject, subject)
}
