package dev.sitar.kmail.runner

import dev.sitar.kmail.smtp.InternetMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter

fun Flow<InternetMessage>.filterSpam(): Flow<InternetMessage> = filter {
    // TODO: filter spam
    true
}