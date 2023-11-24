import dev.sitar.kmail.message.Message
import dev.sitar.kmail.message.message
import kotlinx.datetime.Instant
import kotlinx.datetime.UtcOffset
import kotlin.test.Test
import kotlin.test.assertEquals

private fun String.format() = trimMargin().replace("\n", "\r\n")

class InternetMessageTest {
    val raw = """Message-ID: <58ed1dd5-b4d6-4ec3-b36e-746847bb7e5f@storm.sitar.dev>
            |Date: Tue, 29 Aug 2023 16:28:57 -0400
            |MIME-Version: 1.0
            |User-Agent: Mozilla Thunderbird
            |Content-Language: en-US
            |To: marco@storm.sitar.dev
            |From: marco <marco@storm.sitar.dev>
            |Subject: asd
            |Content-Type: text/plain; charset=UTF-8; format=flowed
            |Content-Transfer-Encoding: 7bit
            |
            |asd
            |""".format()

    val typed = message {
        headers {
            messageId("<58ed1dd5-b4d6-4ec3-b36e-746847bb7e5f@storm.sitar.dev>")
            originationDate(Instant.fromEpochMilliseconds(1693340937000), UtcOffset(-4))
            header("MIME-Version", "1.0")
            header("User-Agent", "Mozilla Thunderbird")
            header("Content-Language", "en-US")
            to("marco@storm.sitar.dev")
            from("marco <marco@storm.sitar.dev>")
            subject("asd")
            header("Content-Type", "text/plain; charset=UTF-8; format=flowed")
            header("Content-Transfer-Encoding", "7bit")
        }

        body {
            line("asd")
        }
    }

    @Test
    fun createMessage() {
        assertEquals(raw, typed.asText())
    }

    @Test
    fun parseMessage() {
        assertEquals(Message.fromText(raw), typed)
    }
}