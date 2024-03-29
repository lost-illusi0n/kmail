package dev.sitar.kmail.runner.storage.filesystems

import dev.sitar.kmail.runner.resolve
import dev.sitar.kmail.runner.storage.Attributable
import dev.sitar.kmail.runner.storage.Attributes
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger { }

class LocalFileSystem(dir: String): FileSystem {
    val root = resolve(dir)

    override suspend fun init() {
        root.mkdir()
    }

    fun readFile(name: String): ByteArray? {
        val file = root.resolve(name)
        return if (file.exists()) file.readBytes() else null
    }

    override fun folder(name: String): FsFolder {
        return LocalFolder(root.resolve(name).also { it.mkdir() })
    }
}

class LocalAttributes(val folder: LocalFolder): Attributes {
    override suspend fun get(name: String): String? {
        return folder.readFile("KMAIL_$name")?.decodeToString()
    }

    override suspend fun set(name: String, value: String) {
        folder.writeFile("KMAIL_$name", value.encodeToByteArray())
    }

    override suspend fun append(name: String, value: String): String {
        folder.file.resolve(name).also { it.createNewFile() }.writer().appendLine(value)
        return folder.file.resolve(name).readText()
    }
}

class LocalFolder(val file: File): FsFolder, Attributable {
    override val name: String = file.name

    override val attributes: Attributes = LocalAttributes(this)

    override fun folder(name: String): FsFolder {
        return LocalFolder(file.resolve(name))
    }

    override suspend fun createFolder(name: String): FsFolder {
        logger.trace { "creating folder $name in ${file.path}" }

        return LocalFolder(file.resolve(name).also { it.mkdir() })
    }

    override suspend fun getFile(name: String): FsFile {
        val file = file.resolve(name)
        return FsFile(file.name, file.length())
    }

    override suspend fun listFiles(): List<FsFile> {
        return file.listFiles { file -> !file.name.startsWith("KMAIL_") }.orEmpty().map { FsFile(it.name, it.length()) }
    }

    override suspend fun listFolders(): List<FsFolder> {
        return file.listFiles { file -> file.isDirectory }.orEmpty().map { LocalFolder(it) }
    }

    override suspend fun readFile(name: String): ByteArray? {
        logger.trace { "reading from $name in ${file.path}" }

        return file.resolve(name).takeIf { it.exists() }?.readBytes()
    }

    override suspend fun writeFile(name: String, contents: ByteArray): FsFile {
        logger.trace { "writing to $name in ${file.path}" }

        val file = file.resolve(name).also { it.createNewFile() }
        file.writeBytes(contents)
        return FsFile(file.name, file.length())
    }

    override suspend fun move(file: String, folder: FsFolder) {
        require(folder is LocalFolder)

        logger.trace { "moving $file from ${this.file.path} to ${folder.file.path}" }

        this.file.resolve(file).renameTo(folder.file.resolve(file))
    }

    override suspend fun rename(from: String, to: String) {
        logger.trace { "renaming $from to $to in ${file.path}" }
        file.resolve(from).renameTo(file.resolve(to))
    }
}

//class LocalFile(val file: File) : FsFile {
//    override val name: String = file.name
//    override val size: Long = file.length()
//
//    override suspend fun rename(name: String) {
//        file.renameTo(file.resolveSibling(name))
//    }
//
//    override suspend fun readContent(): ByteArray {
//        return file.readBytes()
//    }
//}