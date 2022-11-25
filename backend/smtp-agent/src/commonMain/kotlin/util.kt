package dev.sitar.kmail.smtp.agent

import dev.sitar.kmail.smtp.InternetMessage

val InternetMessage.queueId get() = hashCode().toUInt().toString(16)