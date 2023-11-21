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

    override suspend fun getFile(name: String): FsFile? {
        return files.find { it.metadata.name == name }?.metadata
    }

    override suspend fun listFiles(): List<FsFile> {
        return files.map { it.metadata }
    }

    override suspend fun listFolders(): List<FsFolder> {
        return folders
    }

    override suspend fun readFile(name: String): ByteArray? {
        return files.find { it.metadata.name == name }?.content
    }

    override suspend fun writeFile(name: String, contents: ByteArray): FsFile {
        val file = InMemoryFile(FsFile(name, contents.size.toLong()), contents)
        files.add(file)
        return file.metadata
    }

    override suspend fun move(file: String, folder: FsFolder) {
        require(folder is InMemoryFolder)

        val file = files.find { it.metadata.name == file } ?: return
        files.remove(file)
        folder.files.add(file)
    }

    override suspend fun rename(from: String, to: String) {
        val file = files.find { it.metadata.name == from } ?: return
        files.remove(file)
        files.add(InMemoryFile(FsFile(to, file.metadata.size), file.content))
    }
}

class InMemoryFile(val metadata: FsFile, val content: ByteArray)