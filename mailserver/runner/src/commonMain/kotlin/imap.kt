package dev.sitar.kmail.runner

import dev.sitar.kmail.imap.PartSpecifier
import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.agent.ImapFolder
import dev.sitar.kmail.imap.agent.ImapQueryAgent
import dev.sitar.kmail.imap.agent.ImapServer
import dev.sitar.kmail.imap.agent.SelectedMailbox
import dev.sitar.kmail.imap.agent.transports.ImapServerTransportClient
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.imap.frames.response.FlagsResponse
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.message.headers.from
import dev.sitar.kmail.message.headers.messageId
import dev.sitar.kmail.message.headers.subject
import dev.sitar.kmail.message.headers.toRcpt
import dev.sitar.kmail.message.message
import io.ktor.util.*
import mu.KotlinLogging
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

private val logger = KotlinLogging.logger { }

suspend fun imapServer(client: ImapServerTransportClient): ImapServer {
    logger.info("Starting IMAP server.")
    val server = ImapServer(client.bind(), KmailImapQueryAgent, coroutineContext)
    server.listen()
    logger.info("Started IMAP server.")
    return server
}

object KmailImapQueryAgent: ImapQueryAgent {
    override fun select(mailbox: String): SelectedMailbox {
        return SelectedMailbox(
            FlagsResponse.SYSTEM_FLAGS, 11, 2, 123, 20
        )
    }

    override fun create(mailbox: String) {
        println("creating a mailbox called $mailbox")
    }

    override fun listFolders(referenceName: String, forMailbox: String): List<ImapFolder> {
        return listOf(
            ImapFolder(listOf(), "INBOX"), ImapFolder(listOf(), "Sent"), ImapFolder(listOf(), "Trash")
        )
    }

    override fun listSubscribedFolders(referenceName: String, forMailbox: String): List<ImapFolder> {
        return listOf(
//            ImapFolder(listOf(), "INBOX"),
        )
    }

    override fun fetch(
        mailbox: String, sequence: Sequence, dataItems: List<DataItem.Fetch>
    ): Map<Int, List<DataItem.Response>> = sequence.map {
        val message = randomMessage()

        buildList {
            // if sequence is UID the UID response is implicit
            if (sequence.mode == Sequence.Mode.Uid && DataItem.Fetch.Uid !in dataItems) {
                add(DataItem.Response.Uid(it.toString()))
            }

            for (item in dataItems) when (item) {
                DataItem.Fetch.Flags -> add(DataItem.Response.Flags(listOf("\\RECENT")))
                DataItem.Fetch.Rfc822Size -> add(DataItem.Response.Rfc822Size(message.size))
                DataItem.Fetch.Uid -> add(DataItem.Response.Uid(it.toString()))
                is DataItem.Fetch.BodyPeek -> for (part in item.parts) when (part) {
                    is PartSpecifier.Fetch.HeaderFields -> add(
                        DataItem.Response.BodyPeek(
                            listOf(
                                PartSpecifier.Response.HeaderFields(message.headers.filter { header -> part.specifiedFields.any { it.contentEquals(header.fieldName, ignoreCase = true) } })
                            )
                        )
                    )
                }
            }
        }
    }

    fun <T> Sequence.map(block: (id: Int) -> T): Map<Int, T> {
        return buildMap { repeat(19) { put(it + 1, block(it + 1)) } }
    }

    private fun randomMessage(): Message = message {
        headers {
            +from("Joe <joe@bob.com>")
            +toRcpt("Bob <bob@joe.com>")
            +subject("a message ${Random.nextBytes(20).encodeBase64().removeSuffix("=")}")
            +messageId("<${Random.nextBytes(20).encodeBase64().removeSuffix("=")}@mail.bob.com>")
        }

        body {
            line("asdasd")
        }
    }
}

private val Message.size: Int
    get() = headers.fold(0) { a, b -> a + b.fieldBody.length + 2 + b.fieldBody.length + 2 } + (body?.length ?: 0)