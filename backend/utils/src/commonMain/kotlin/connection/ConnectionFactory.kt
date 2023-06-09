package dev.sitar.kmail.utils.connection

interface ConnectionFactory {
    suspend fun connect(host: String, port: Int): Connection
}