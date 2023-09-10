package dev.sitar.kmail.runner.storage

//class KmailInMemoryStorageLayer : StorageLayer {
//    private val userStorages = mutableMapOf<String, UserStorageLayer>()
//
//    override suspend fun user(username: String): UserStorageLayer {
//        if (Config.accounts.none { it.username.contentEquals(username) }) throw Exception("User does not exist.")
//
//        return userStorages.getOrPut(username) { KmailInMemoryUserStorageLayer() }
//    }
//}
//
//class KmailInMemoryUserStorageLayer : UserStorageLayer {
//    private val mailboxes: MutableList<KmailInMemoryUserDirectoryStorageLayer> = mutableListOf()
//
//    override suspend fun mailbox(name: String?): UserDirectoryStorageLayer {
//        val mailbox = KmailInMemoryUserDirectoryStorageLayer(name ?: "root")
//        mailboxes.add(mailbox)
//        return mailbox
//    }
//
//    override suspend fun mailboxes(): List<UserDirectoryStorageLayer> = mailboxes
//}
//
//class KmailInMemoryUserDirectoryStorageLayer(override val name: String) : UserDirectoryStorageLayer {
//    private val messages: Channel<Message> = Channel()
//
//    override suspend fun store(message: Message) {
//        messages.send(message)
//    }
//
//    override fun messages(): List<Message> {
//        return messages.asList()
//    }
//}
//
//@OptIn(ExperimentalCoroutinesApi::class)
//private fun <T> ReceiveChannel<T>.asList(): MutableList<T> {
//    return buildList {
//        while (!isClosedForReceive) {
//            add(tryReceive().getOrNull() ?: break)
//        }
//    }.toMutableList()
//}