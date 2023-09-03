package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.sasl.SaslChallenge

interface ImapLayer {
    suspend fun authenticate(challenge: SaslChallenge): String?

    suspend fun mailbox(username: String): ImapMailbox

    suspend fun create(username: String, mailbox: String)
}

data class LightImapFolder(val attributes: Set<String>, val name: String)

interface ImapMailbox {
    suspend fun folders(): List<LightImapFolder>

    fun folder(name: String): ImapFolder?

    suspend fun createFolder(name: String)

    suspend fun subscriptions(): List<String>

    suspend fun subscribe(folder: String)
}

data class SelectedMailbox(val flags: Set<String>, val exists: Int, val recent: Int, val uidValidity: Int, val uidNext: Int)