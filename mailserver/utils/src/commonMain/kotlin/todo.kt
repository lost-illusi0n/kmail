package dev.sitar.kmail.utils

class NotImplementedException(message: String) : Exception(message)

// exception instead of an error
fun todo(message: String): Nothing {
    throw NotImplementedException("An operation is not implemented: $message")
}

fun todo(): Nothing {
    throw NotImplementedException("An operation is not implemented.")
}