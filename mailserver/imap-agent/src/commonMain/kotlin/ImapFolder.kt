package dev.sitar.kmail.imap.agent

data class ImapFolder(val attributes: List<String>, val name: String) {
    companion object {
        const val DELIM = "/"
    }
}
