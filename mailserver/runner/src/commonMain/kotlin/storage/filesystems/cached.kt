package dev.sitar.kmail.runner.storage.filesystems

import dev.sitar.kmail.runner.storage.Attributes

class CachedFileSystem(val fs: FileSystem): FileSystem {
    val folders = mutableListOf<FsFolder>()

    override suspend fun init() {
        fs.init()
    }

    override fun folder(name: String): FsFolder {
        return folders.find { it.name == name } ?: CachedFolder(fs.folder(name)).also { folders.add(it) }
    }
}

class CachedAttributes(val attributes: Attributes) : Attributes {
    val cached = mutableMapOf<String, String>()

    override suspend fun get(name: String): String? {
        return cached[name] ?: attributes.get(name)?.also { cached[name] = it }
    }

    override suspend fun set(name: String, value: String) {
        cached[name] = value
        attributes.set(name, value)
    }

    override suspend fun append(name: String, value: String): String {
        val cachedValue = cached[name]

        if (cachedValue != null) {
            val newValue = "$cachedValue$value\n"
            set(name, newValue)
            return newValue
        } else {
            val newValue = attributes.append(name, value)
            cached[name] = newValue
            return newValue
        }
    }
}

class CachedFolder(val folder: FsFolder): FsFolder {
    override val name: String = folder.name

    override val attributes: Attributes = CachedAttributes(folder.attributes)

    private var hasRetrievedFolders = false
    var folders = mutableListOf<FsFolder>()
        private set

    private var hasRetrievedFiles = false
    var files = mutableListOf<FsFile>()
        private set

    override fun folder(name: String): FsFolder {
        return folders.find { it.name == name } ?: CachedFolder(folder.folder(name)).also { folders.add(it) }
    }

    override suspend fun createFolder(name: String): FsFolder {
        return CachedFolder(folder.createFolder(name)).also { folders.add(it) }
    }

    override suspend fun listFiles(): List<FsFile> {
        if (hasRetrievedFiles) {
            println("hit cache")
            return files
        }
        else {
            println("cache miss")
            files = folder.listFiles().map { CachedFile(it.name, it.size) { it.readContent() } }.toMutableList()
            hasRetrievedFiles = true
            return files
        }
    }

    override suspend fun listFolders(): List<FsFolder> {
        if (hasRetrievedFolders) return folders
        else {
            folders = folder.listFolders().map { CachedFolder(it) }.toMutableList()
            hasRetrievedFolders = true
            return folders
        }
    }

    override suspend fun readFile(name: String): ByteArray? {
        return files.find { it.name == name }?.readContent() ?: folder.readFile(name)?.also { files.add(CachedFile(name, it.size.toLong()) { it }) }
    }

    override suspend fun writeFile(name: String, contents: ByteArray): FsFile {
        folder.writeFile(name, contents)

        val cached = CachedFile(name, contents.size.toLong()) { contents }
        files.add(cached)
        return cached
    }

    override suspend fun move(file: String, folder: FsFolder) {
        require(folder is CachedFolder)

        folder.move(file, folder.folder)

        val file = files.find { it.name == file }!!
        files.remove(file)
        folder.files.add(file)
    }
}

class CachedFile(override val name: String, override val size: Long, val content: suspend () -> ByteArray) : FsFile {
    override suspend fun readContent(): ByteArray {
        return content()
    }
}