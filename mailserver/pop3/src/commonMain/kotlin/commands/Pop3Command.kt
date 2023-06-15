package dev.sitar.kmail.pop3.commands

sealed interface Pop3Command

object UnknownCommand: Pop3Command