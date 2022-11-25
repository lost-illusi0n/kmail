package dev.sitar.kmail.message.headers



fun from(from: String): Header {
    return Header(Headers.From, from)
}