package dev.sitar.kmail.imap.agent

enum class ImapState {
    NotAuthenticated,
    Authenticated,
    Selected,
    Logout
}