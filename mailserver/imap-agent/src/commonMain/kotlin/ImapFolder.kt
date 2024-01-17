package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.PartSpecifier
import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.imap.frames.command.StoreMode
import dev.sitar.kmail.message.Message
import dev.sitar.kmail.utils.todo
import kotlin.math.max
import kotlin.math.min

data class LightImapFolder(val attributes: Set<String>, val name: String)

interface ImapMailbox {
    suspend fun folders(): List<LightImapFolder>

    fun folder(name: String): ImapFolder?

    suspend fun createFolder(name: String)

    suspend fun subscriptions(): List<String>

    suspend fun subscribe(folder: String)

    suspend fun unsubscribe(folder: String)
}

sealed class Flag(val value: String) {
    object Replied: Flag("\\ANSWERED")
    object Seen: Flag("\\SEEN")
    object Trashed: Flag("\\DELETED")
    object Draft: Flag("\\DRAFT")
    object Flagged: Flag("\\FLAGGED")
    object Recent: Flag("\\RECENT")
    class Other(value: String): Flag(value) {
        override fun toString(): String {
            return "Other(value=$value)"
        }
    }

    companion object {
        private val values by lazy { arrayOf(Replied, Seen, Trashed, Draft, Flagged, Recent) }

        fun fromValue(value: String): Flag {
            return values.find { it.value.contentEquals(value, ignoreCase = true) } ?: Other(value)
        }
    }
}

data class ImapMessage(
    val uniqueIdentifier: Int,
    val sequenceNumber: Int,
    val flags: Set<Flag>,
    val size: Long,
    val retrieve: suspend () -> Message
)

// TODO: maybe allow to lock?
interface ImapFolder {
    val name: String

    val attributes: Set<String>
    val flags: Set<String>

    suspend fun onMessageStore(handler: (suspend (ImapMessage) -> Unit)?)

    suspend fun exists(): Int
    suspend fun recent(): Int

    //    val unseen: Int
    suspend fun uidValidity(): Int

    suspend fun setUidValidity(value: Int)

    suspend fun uidNext(): Int

    suspend fun messages(): List<ImapMessage>

    suspend fun save(flags: Set<Flag>, message: String)

    suspend fun store(sequence: Sequence, flags: Set<Flag>, mode: StoreMode, messagesSnapshot: List<ImapMessage>? = null): Map<Int, Set<Flag>>

    suspend fun fetch(sequence: Sequence, dataItems: List<DataItem.Fetch>): Map<Int, Set<DataItem.Response>> {
        val messagesSnapshot = messages()

        val selectedMessages = sequenceToMessages(sequence, messagesSnapshot).takeIf { it.isNotEmpty() } ?: return emptyMap()

        if (dataItems.any { it is DataItem.Fetch.Body }) store(sequence, setOf(Flag.Seen), StoreMode.Add, messagesSnapshot = messagesSnapshot)

        return selectedMessages.associate { message ->
            val pos = when (sequence.mode) {
                Sequence.Mode.Sequence -> message.sequenceNumber
                Sequence.Mode.Uid -> message.uniqueIdentifier
            }

            pos to buildSet {
                // if sequence is UID the UID response is implicit
                if (sequence.mode == Sequence.Mode.Uid && DataItem.Fetch.Uid !in dataItems) {
                    add(DataItem.Response.Uid(message.uniqueIdentifier.toString()))
                }

                for (item in dataItems) when (item) {
                    DataItem.Fetch.Flags -> add(DataItem.Response.Flags(message.flags.map { it.value }))
                    DataItem.Fetch.Rfc822Size -> add(DataItem.Response.Rfc822Size(message.size))
                    DataItem.Fetch.Uid -> add(DataItem.Response.Uid(message.uniqueIdentifier.toString()))
                    is DataItem.Fetch.BodyType -> {
                        val typed = message.retrieve()

                        if (item.parts.isEmpty()) {
                            add(DataItem.Response.Body(PartSpecifier.Response.Body(typed)))
                        }

                        for (part in item.parts) when (part) {
                            // TODO: handle peek/read
                            is PartSpecifier.Fetch.HeaderFields -> add(
                                DataItem.Response.Body(
                                    PartSpecifier.Response.HeaderFields(typed.headers.filter { header ->
                                        part.specifiedFields.any {
                                            it.contentEquals(
                                                header.fieldName,
                                                ignoreCase = true
                                            )
                                        }
                                    }.map { it.copy(it.fieldName.uppercase()) })
                                )
                            )

                            PartSpecifier.Fetch.Header -> {
                                add(DataItem.Response.Body(PartSpecifier.Response.Header(typed.headers.toList())))
                            }

                            PartSpecifier.Fetch.Text -> {
                                add(DataItem.Response.Body(PartSpecifier.Response.Text(typed.body ?: "")))
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun sequenceToMessages(
        sequence: Sequence,
        messagesSnapshot: List<ImapMessage>? = null
    ): List<ImapMessage> {
        val messagesSnapshot = messagesSnapshot ?: messages()

        val start = when (sequence) {
            is Sequence.Set -> with(sequence.start) {
                when (this) {
                    is Sequence.Position.Actual -> pos
                    Sequence.Position.Any -> todo()
                }
            }

            is Sequence.Single -> with(sequence.pos) {
                when (this) {
                    is Sequence.Position.Actual -> pos
                    Sequence.Position.Any -> todo()
                }
            }
        }

        val end = when (sequence) {
            is Sequence.Set -> with(sequence.end) {
                when (this) {
                    is Sequence.Position.Actual -> pos
                    Sequence.Position.Any -> messagesSnapshot.size
                }
            }

            is Sequence.Single -> with(sequence.pos) {
                when (this) {
                    is Sequence.Position.Actual -> pos
                    Sequence.Position.Any -> todo()
                }
            }
        }

        val exists = exists()

        if (!(start in 0..exists && end in 0..exists)) return listOf()

        return when (sequence.mode) {
            Sequence.Mode.Sequence -> {
                messagesSnapshot.subList(messagesSnapshot.size - end, messagesSnapshot.size + 1 - start)
            }

            Sequence.Mode.Uid -> {
                val a = messagesSnapshot.indexOfFirst { it.uniqueIdentifier == start }
                val b = messagesSnapshot.indexOfFirst { it.uniqueIdentifier == end }

                val start = min(a, b)
                val end = max(a, b)
                messagesSnapshot.subList(start, end + 1)
            }
        }
    }

    companion object {
        const val DELIM = "/"
    }
}
