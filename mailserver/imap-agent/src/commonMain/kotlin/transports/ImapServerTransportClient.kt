package dev.sitar.kmail.imap.agent.transports

interface ImapServerTransportClient {
    fun bind(): ImapServerTransport
}