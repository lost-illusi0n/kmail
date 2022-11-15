package dev.sitar.kmail.message.headers

fun toRcpt(recipient: String): Header {
    return Header("To", recipient)
}