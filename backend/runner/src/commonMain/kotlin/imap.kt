package dev.sitar.kmail.runner

import dev.sitar.kmail.imap.SequenceSet
import dev.sitar.kmail.imap.agent.ImapFolder
import dev.sitar.kmail.imap.agent.ImapQueryAgent
import dev.sitar.kmail.imap.agent.ImapServer
import dev.sitar.kmail.imap.agent.transports.ImapServerTransportClient
import dev.sitar.kmail.imap.frames.DataItem
import mu.KotlinLogging
import kotlin.coroutines.coroutineContext

private val logger = KotlinLogging.logger { }

suspend fun imapServer(client: ImapServerTransportClient): ImapServer {
    logger.info("Starting IMAP server.")
    val server = ImapServer(client.bind(), KmailImapQueryAgent, coroutineContext)
    server.listen()
    logger.info("Started IMAP server.")
    return server
}

object KmailImapQueryAgent: ImapQueryAgent {
    override fun listFolders(referenceName: String, forMailbox: String): List<ImapFolder> {
        return listOf(
            ImapFolder(listOf(), "INBOX"),
            ImapFolder(listOf(), "Sent"),
            ImapFolder(listOf(), "bloboboba")
        )
    }

    override fun listSubscribedFolders(referenceName: String, forMailbox: String): List<ImapFolder> {
        return listOf(
//            ImapFolder(listOf(), "INBOX"),
        )
    }

    override fun fetch(mailbox: String, sequence: SequenceSet, dataItems: List<String>): List<Map<String, String>> = buildList {
        add(buildMap {
            if (mailbox == "INBOX") {
                if (sequence.mode == SequenceSet.Mode.Uid) {
                    put(DataItem.UID, "123")
                }

                if (DataItem.FLAGS in dataItems) {
                    put(DataItem.FLAGS, "(\\RECENT)")
                }
            }
        })
    }
}