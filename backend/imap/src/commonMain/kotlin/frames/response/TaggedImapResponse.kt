package dev.sitar.kmail.imap.frames.response

import dev.sitar.kmail.imap.frames.Tag

data class TaggedImapResponse(val tag: Tag, val response: ImapResponse)

operator fun Tag.plus(response: ImapResponse): TaggedImapResponse {
    return TaggedImapResponse(this, response)
}
