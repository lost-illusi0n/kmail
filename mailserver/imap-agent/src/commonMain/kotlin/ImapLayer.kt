package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.frames.DataItem

interface ImapLayer {
    suspend fun login(username: String, password: String): Boolean

    suspend fun select(username: String, mailbox: String): ImapMailbox?

    suspend fun create(username: String, mailbox: String)

    suspend fun mailboxes(username: String): List<ImapMailbox>

    fun listFolders(referenceName: String, forMailbox: String): List<ImapFolder>

    fun listSubscribedFolders(referenceName: String, forMailbox: String): List<ImapFolder>

    fun fetch(mailbox: String, sequence: Sequence, dataItems: List<DataItem.Fetch>): Map<Int, List<DataItem.Response>>
}

interface ImapMailbox {
    val name: String

    val flags: Set<String>
    val exists: Int
    val recent: Int
    val uidValidity: Int
    val uidNext: Int
}

data class SelectedMailbox(val flags: Set<String>, val exists: Int, val recent: Int, val uidValidity: Int, val uidNext: Int)