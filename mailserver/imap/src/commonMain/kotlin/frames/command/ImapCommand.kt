package dev.sitar.kmail.imap.frames.command

import dev.sitar.kio.async.readers.AsyncReader

sealed interface ImapCommand {
    val identifier: Identifier

    enum class Identifier(val serializer: ImapCommandSerializer<out ImapCommand>) {
        Capability(CapabilityCommand),
        Logout(LogoutCommand),
        Noop(NoOpCommand),
        StartTls(StartTlsCommand),
        Authenticate(AuthenticateCommand),
        Select(SelectCommand),
        Uid(UidCommand),
        Login(LoginCommand),
        List(ListCommand),
        Lsub(ListSubscriptionsCommand),
        Subscribe(SubscribeCommand),
        Unsubscribe(UnsubscribeCommand),
        Fetch(FetchCommand),
        Create(CreateCommand),
        Status(StatusCommand),
        Notify(NotifyCommand),
        Idle(IdleCommand),
        Check(CheckCommand),
        Append(AppendCommand);

        companion object {
            fun findByIdentifier(identifier: String) : Identifier? {
                return values().find { it.name.equals(identifier, ignoreCase = true) }
            }
        }
    }
}

interface ImapCommandSerializer<T: ImapCommand> {
    suspend fun deserialize(input: AsyncReader): T
}