package dev.sitar.kmail.imap.agent

import dev.sitar.kmail.imap.PartSpecifier
import dev.sitar.kmail.imap.Sequence
import dev.sitar.kmail.imap.frames.DataItem
import dev.sitar.kmail.message.Message
import kotlin.math.max
import kotlin.math.min

interface ImapMessage {
    val uniqueIdentifier: Int
    val sequenceNumber: Int

    val size: Long

    suspend fun typedMessage(): Message
}

interface ImapFolder {
    val name: String

    val attributes: Set<String>
    val flags: Set<String>

    suspend fun exists(): Int
    suspend fun recent(): Int

    //    val unseen: Int
    suspend fun uidValidity(): Int

    suspend fun setUidValidity(value: Int)

    suspend fun uidNext(): Int

    suspend fun messages(): List<ImapMessage>

    suspend fun fetch(sequence: Sequence, dataItems: List<DataItem.Fetch>): Map<Int, Set<DataItem.Response>> {
        val messagesSnapshot = messages()

        val start = when (sequence) {
            is Sequence.Set -> with(sequence.start) {
                when (this) {
                    is Sequence.Position.Actual -> pos
                    Sequence.Position.Any -> TODO()
                }
            }
            is Sequence.Single -> with(sequence.pos) {
                when (this) {
                    is Sequence.Position.Actual -> pos
                    Sequence.Position.Any -> TODO()
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
                    Sequence.Position.Any -> TODO()
                }
            }
        }

        val exists = exists()

        if (!(start in 0..exists && end in 0..exists)) return emptyMap()

        val selectedMessages = when (sequence.mode) {
            Sequence.Mode.SequenceNumber -> {
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

        return selectedMessages.associate { message ->
            when (sequence.mode) {
                Sequence.Mode.SequenceNumber -> message.sequenceNumber
                Sequence.Mode.Uid -> message.uniqueIdentifier
            } to buildSet {
                // if sequence is UID the UID response is implicit
                if (sequence.mode == Sequence.Mode.Uid && DataItem.Fetch.Uid !in dataItems) {
                    add(DataItem.Response.Uid(message.uniqueIdentifier.toString()))
                }

                for (item in dataItems) when (item) {
                    DataItem.Fetch.Flags -> add(DataItem.Response.Flags(listOf("\\Recent")))
                    DataItem.Fetch.Rfc822Size -> add(DataItem.Response.Rfc822Size(message.size))
                    DataItem.Fetch.Uid -> add(DataItem.Response.Uid(message.uniqueIdentifier.toString()))
                    is DataItem.Fetch.BodyType -> {
                        val typed = message.typedMessage()

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

                            is PartSpecifier.Fetch.Header -> {
                                add(DataItem.Response.Body(PartSpecifier.Response.Header(typed.headers.toList())))
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val DELIM = "/"
    }
}
