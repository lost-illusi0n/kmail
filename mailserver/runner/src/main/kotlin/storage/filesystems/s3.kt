package dev.sitar.kmail.runner.storage.filesystems

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.Object
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.net.Url
import dev.sitar.kmail.runner.KmailConfig
import dev.sitar.kmail.runner.storage.Attributable
import dev.sitar.kmail.runner.storage.Attributes
import mu.KotlinLogging

private interface S3Context {
    val config: KmailConfig.Mailbox.Filesystem.S3
    val client: S3Client
}

private val logger = KotlinLogging.logger { }

class S3FileSystem(override val config: KmailConfig.Mailbox.Filesystem.S3): FileSystem, S3Context {
    override val client = S3Client {
        region = config.region.value

        if (config.backend != null) {
            endpointUrl = Url.parse(config.backend.endpoint)

            forcePathStyle = config.backend.pathStyleAccessEnabled
        }

        if (config.credentials != null) {
            credentialsProvider = StaticCredentialsProvider(Credentials(config.credentials.username, config.credentials.password))
        }
    }

    override suspend fun init() {
        logger.debug { "Initiating the S3 filesystem." }

        if (!client.bucketExists(config.bucket.name)) {
            logger.debug { "Bucket specified does not exist. Creating ${config.bucket.name}." }

            client.createBucket {
                bucket = config.bucket.name
                createBucketConfiguration {
                    locationConstraint = config.bucket.location
                }
            }
        }

        if (config.rootFolder != null) {
            if (!client.keyExists(config.bucket.name, "${config.rootFolder}/")) {
                logger.debug { "Root folder does not exist. Creating ${config.rootFolder}." }

                client.putObject {
                    bucket = config.bucket.name
                    key = "${config.rootFolder}/"
                }
            }
        }
    }

    override fun folder(name: String): FsFolder {
        return S3Folder(this, Key.fromFs(this, name))
    }
}

class Key(val folders: List<String>) {
    companion object {
        fun fromFs(fs: S3FileSystem, folder: String): Key {
            return if (fs.config.rootFolder != null)
                Key(listOf(fs.config.rootFolder, folder))
            else
                Key(listOf(folder))
        }
    }

    fun append(folder: String): Key = Key(folders + folder)

    override fun toString(): String {
        return folders.joinToString(separator = "/", postfix = "/")
    }
}

class S3Attributes(val folder: S3Folder) : Attributes, S3Context by folder {
    override suspend fun get(name: String): String? {
        return folder.readFile("KMAIL_$name")?.decodeToString()
    }

    override suspend fun set(name: String, value: String) {
        folder.writeFile("KMAIL_$name", value.encodeToByteArray())
    }
}

class S3Folder(val fileSystem: S3FileSystem, val folderKey: Key): Attributable, FsFolder, S3Context by fileSystem {
    override val name: String = folderKey.folders.last()

    override val attributes: Attributes = S3Attributes(this)

    override fun folder(name: String): S3Folder {
        return S3Folder(fileSystem, folderKey.append(name))
    }

    override suspend fun createFolder(name: String): S3Folder {
        client.putObject {
            bucket = config.bucket.name
            this.key = "$folderKey$name/"
        }

        return S3Folder(fileSystem, folderKey.append(name))
    }

    override suspend fun getFile(name: String): FsFile {
        val obj = client.headObject {
            this.bucket = config.bucket.name
            this.key = "$folderKey$name"
        }

        return FsFile(name, obj.contentLength)
    }

    override suspend fun listFolders(): List<FsFolder> {
        // TODO: parse key
        return client.listObjectsV2 {
            bucket = config.bucket.name
            this.delimiter = "/"
            this.prefix = folderKey.toString()
        }.contents.orEmpty()
            .filter { it.isFolder() }
            .map { S3Folder(fileSystem, folderKey.append(it.key!!.split('/').last())) }
    }

    private fun Object.isFolder(): Boolean {
        return key!!.endsWith("/") && key!!.split('/').size > 3
    }

    override suspend fun listFiles(): List<FsFile> {
        return client.listObjectsV2 {
            bucket = config.bucket.name
            this.delimiter = "/"
            this.prefix = folderKey.toString()
        }.contents.orEmpty().filterNot { it.isNotAcceptableFile() }.map { FsFile(it.key!!.split('/').last(), it.size) }
    }

    private fun Object.isNotAcceptableFile(): Boolean {
        return key!!.split('/').last().startsWith("kmail_") && key == folderKey.toString()
    }

    override suspend fun readFile(name: String): ByteArray? {
        val request = GetObjectRequest {
            this.bucket = config.bucket.name
            this.key = "$folderKey$name"
        }

        return try {
            client.getObject(request) { it.body?.toByteArray() }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun writeFile(name: String, contents: ByteArray): FsFile {
        client.putObject {
            bucket = config.bucket.name
            key = "$folderKey$name"
            body = ByteStream.fromBytes(contents)
        }

        return FsFile("$folderKey$name", contents.size.toLong())
    }

    override suspend fun move(file: String, folder: FsFolder) {
        require(folder is S3Folder)

        client.copyObject {
            bucket = config.bucket.name
            key = "${folder.folderKey}$file"
            copySource = "/${config.bucket.name}/$folderKey$file"
        }

        client.deleteObject {
            bucket = config.bucket.name
            this.key = "$folderKey$file"
        }
    }

    override suspend fun rename(from: String, to: String) {
        client.copyObject {
            bucket = config.bucket.name
            key = "$folderKey$to"
            copySource = "/${config.bucket.name}/$folderKey$from"
        }

        client.deleteObject {
            bucket = config.bucket.name
            this.key = "$folderKey$from"
        }
    }
}

//class S3File(val folder: S3Folder, override val name: String, override val size: Long): FsFile, S3Context by folder {
//    override suspend fun rename(name: String) {
//        client.copyObject {
//            bucket = config.bucket.name
//            key = "${folder.folderKey}$name"
//            copySource = "/${config.bucket.name}/${folder.folderKey}${this@S3File.name}"
//        }
//
//        client.deleteObject {
//            bucket = config.bucket.name
//            this.key = "${folder.folderKey}${this@S3File.name}"
//        }
//    }
//
//    override suspend fun readContent(): ByteArray {
//        return folder.readFile(name)!!
//    }
//}

private suspend fun S3Client.bucketExists(bucket: String) = try {
    headBucket { this.bucket = bucket }
    true
} catch (e: Exception) {
    false
}


suspend fun S3Client.keyExists(bucket: String, key: String) = try {
    headObject {
        this.bucket = bucket
        this.key = key
    }
    true
} catch (e: Exception) {
    false
}