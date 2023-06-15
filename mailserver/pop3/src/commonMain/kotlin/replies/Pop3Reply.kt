package dev.sitar.kmail.pop3.replies

sealed interface Pop3Reply {
    data class DataReply(override val message: String) : Pop3Reply
    data class OkReply(override val message: String) : Pop3Reply
    data class ErrReply(override val message: String) : Pop3Reply

    val message: String
}