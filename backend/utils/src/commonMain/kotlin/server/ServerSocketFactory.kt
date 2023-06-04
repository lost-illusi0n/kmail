package dev.sitar.kmail.utils.server

interface ServerSocketFactory {
    suspend fun bind(port: Int): ServerSocket
}