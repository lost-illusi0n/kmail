package dev.sitar.kmail.agents.smtp

import dev.sitar.kmail.smtp.InternetMessage

val InternetMessage.queueId get() = hashCode().toUInt().toString(16)