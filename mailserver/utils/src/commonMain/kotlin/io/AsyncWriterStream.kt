package dev.sitar.kmail.utils.io

import dev.sitar.kio.async.writers.AsyncWriter

interface AsyncWriterStream : AsyncWriter {
    fun flush()
}