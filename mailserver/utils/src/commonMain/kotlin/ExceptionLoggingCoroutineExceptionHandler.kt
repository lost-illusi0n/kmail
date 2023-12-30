package dev.sitar.kmail.utils

import kotlinx.coroutines.CoroutineExceptionHandler
import mu.KLogger

fun ExceptionLoggingCoroutineExceptionHandler(logger: KLogger): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, throwable -> logger.error(throwable) { "Kmail encountered the following exception." } }
}