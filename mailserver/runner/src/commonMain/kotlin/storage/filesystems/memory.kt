package dev.sitar.kmail.runner.storage.filesystems

import dev.sitar.kmail.runner.storage.Attributes

class InMemoryFileSystem: FileSystem {
    val folders = mutableListOf<InMemoryFolder>()

    override suspend fun init() { }

    override fun folder(name: String): FsFolder {
        return folders.find { it.name == name } ?: InMemoryFolder(name).also { folders.add(it) }
    }
}

class InMemoryAttributes: Attributes {
    val attributes = mutableMapOf<String, String>()

    override suspend fun get(name: String): String? {
        return attributes[name]
    }

    override suspend fun set(name: String, value: String) {
        attributes[name] = value
    }
}

class InMemoryFolder(override val name: String): FsFolder {
    val files = mutableListOf<InMemoryFile>()
    val folders = mutableListOf<InMemoryFolder>()

    override val attributes: Attributes = InMemoryAttributes()

    override fun folder(name: String): FsFolder {
        return folders.find { it.name == name } ?: InMemoryFolder(name).also { folders.add(it) }
    }

    override suspend fun createFolder(name: String): FsFolder {
        return InMemoryFolder(name).also { folders.add(it) }
    }

    override suspend fun listFiles(): List<FsFile> {
        return files
    }

    override suspend fun listFolders(): List<FsFolder> {
        return folders
    }

    override suspend fun readFile(name: String): ByteArray? {
        return files.find { it.name == name }?.content
    }

    override suspend fun writeFile(name: String, contents: ByteArray): FsFile {
        val file = InMemoryFile(name, contents)
        files.add(file)
        return file
    }

    override suspend fun move(file: String, folder: FsFolder) {
        require(folder is InMemoryFolder)

        val file = files.find { it.name == file } ?: return
        files.remove(file)
        folder.files.add(file)
    }
}

class InMemoryFile(override val name: String, val content: ByteArray): FsFile {
    override val size: Long = content.size.toLong()

    override suspend fun readContent(): ByteArray = content
}