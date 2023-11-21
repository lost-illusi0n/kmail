package dev.sitar.kmail.runner.storage.filesystems

import dev.sitar.kmail.runner.storage.Attributable

interface FileSystem {
    suspend fun init()

    fun folder(name: String): FsFolder
}

interface FsFolder: Attributable {
    val name: String

    fun folder(name: String): FsFolder

    suspend fun createFolder(name: String): FsFolder

    suspend fun getFile(name: String): FsFile?

    suspend fun listFiles(): List<FsFile>

    suspend fun listFolders(): List<FsFolder>

    suspend fun readFile(name: String): ByteArray?

    suspend fun writeFile(name: String, contents: ByteArray): FsFile

    // TODO: change this
    suspend fun move(file: String, folder: FsFolder)

    suspend fun rename(from: String, to: String)
}

data class FsFile(val name: String, val size: Long)