package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.SequenceSet

interface ImapQueryAgent {
    fun listFolders(referenceName: String, forMailbox: String): List<ImapFolder>

    fun listSubscribedFolders(referenceName: String, forMailbox: String): List<ImapFolder>

    fun fetch(mailbox: String, sequence: SequenceSet, dataItems: List<String>): List<Map<String, String>>
}