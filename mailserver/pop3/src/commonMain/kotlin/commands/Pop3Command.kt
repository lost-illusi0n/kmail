package dev.sitar.kmail.pop3.commands

sealed interface Pop3Command

data class UnknownCommand(val line: String): Pop3Command