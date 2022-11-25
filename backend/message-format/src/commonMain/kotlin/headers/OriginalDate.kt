package dev.sitar.kmail.message.headers

import kotlinx.datetime.*
import kotlin.math.absoluteValue
import kotlin.math.sign

fun originalDate(date: Instant, zone: UtcOffset): Header {
    return Header(Headers.OriginalDate, date.format(zone))
}

private fun Instant.format(offset: UtcOffset): String {
    with(toLocalDateTime(FixedOffsetTimeZone(offset))) {
        val offsetInHours = offset.totalSeconds / 60 / 60
        val sign = if (offsetInHours.sign == 1) "+" else "-"
        // FIXME?: why is zone 4 digits. can it account for minutes as well? if so do that
        val zone = "$sign${offsetInHours.absoluteValue.toString().padStart(2, '0').padEnd(4, '0')}"
        val dayOfWeek = dayOfWeek.name.take(3).lowercase().capitalize()
        val month = month.name.take(3).lowercase().capitalize()
        val hour = hour.toString().padStart(2, '0')
        val minute = minute.toString().padStart(2, '0')
        val second = second.toString().padStart(2, '0')

        return "$dayOfWeek, $dayOfMonth $month $year $hour:$minute:$second $zone"
    }
}