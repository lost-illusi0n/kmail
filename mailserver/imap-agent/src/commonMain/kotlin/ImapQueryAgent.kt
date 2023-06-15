package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.frames.DataItem

interface ImapQueryAgent {
    fun select(mailbox: String): SelectedMailbox

    fun create(mailbox: String)

    fun listFolders(referenceName: String, forMailbox: String): List<ImapFolder>

    fun listSubscribedFolders(referenceName: String, forMailbox: String): List<ImapFolder>

    fun fetch(mailbox: String, sequence: Sequence, dataItems: List<DataItem.Fetch>): Map<Int, List<DataItem.Response>>
}

data class SelectedMailbox(val flags: Set<String>, val exists: Int, val recent: Int, val uidValidity: Int, val uidNext: Int)