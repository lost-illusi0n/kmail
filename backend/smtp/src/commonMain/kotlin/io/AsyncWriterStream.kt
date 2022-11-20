package dev.sitar.kmail.smtp.io

import dev.sitar.kio.async.writers.AsyncWriter

public interface AsyncWriterStream : AsyncWriter {
    public fun flush()
}