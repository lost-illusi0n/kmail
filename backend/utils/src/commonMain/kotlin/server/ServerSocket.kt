package dev.sitar.kmail.utils.server

import dev.sitar.kmail.utils.connection.Connection

interface ServerSocket {
    suspend fun accept(): Connection
}