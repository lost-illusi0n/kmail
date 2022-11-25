package dev.sitar.kmail.message.headers

fun messageId(messageId: String): Header {
    return Header(Headers.MessageId, messageId)
}