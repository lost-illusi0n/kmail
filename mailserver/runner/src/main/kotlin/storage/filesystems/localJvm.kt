package dev.sitar.kmail.runner.storage.filesystems

import dev.sitar.kmail.runner.KmailConfig
import dev.sitar.kmail.runner.storage.Attributable
import dev.sitar.kmail.runner.storage.Attributes
import java.io.File

class LocalFileSystem(val config: KmailConfig.Mailbox.Filesystem.Local): FileSystem {
    val root = File(config.dir)

    override suspend fun init() {
        root.mkdir()
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
        return LocalFolder(file.resolve(name).also { it.mkdir() })
    }

    override suspend fun listFiles(): List<FsFile> {
        return file.listFiles { file -> !file.name.startsWith("KMAIL_") }.orEmpty().map { LocalFile(it) }
    }

    override suspend fun listFolders(): List<FsFolder> {
        return file.listFiles { file -> file.isDirectory }.orEmpty().map { LocalFolder(it) }
    }

    override suspend fun readFile(name: String): ByteArray? {
        return file.resolve(name).takeIf { it.exists() }?.readBytes()
    }

    override suspend fun writeFile(name: String, contents: ByteArray): FsFile {
        val file = file.resolve(name).also { it.createNewFile() }
        file.writeBytes(contents)
        return LocalFile(file)
    }

    override suspend fun move(file: String, folder: FsFolder) {
        require(folder is LocalFolder)

        this.file.resolve(file).renameTo(folder.file.resolve(file))
    }
}

class LocalFile(val file: File) : FsFile {
    override val name: String = file.name
    override val size: Long = file.length()

    override suspend fun readContent(): ByteArray {
        return file.readBytes()
    }
}