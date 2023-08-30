package dev.sitar.kmail.pop3.commands

enum class Pop3CommandName(val deserializer: Pop3CommandDeserializer<*>) {
    Quit(QuitCommand.Deserializer),
    Stat(StatCommand.Deserializer),
    List(ListCommand.Deserializer),
    Retr(RetrCommand.Deserializer),
    Dele(DeleCommand.Deserializer),
    Noop(NoopCommand.Deserializer),
    User(UserCommand.Deserializer),
    Rset(RsetCommand.Deserializer),
    Pass(PassCommand.Deserializer),
    Uidl(UidlCommand.Deserializer),
    Capa(CapaCommand.Deserializer);
    // optional
//    Top,
//    Apop

    companion object {
        fun fromName(name: String): Pop3CommandName? {
            return values().find { it.name.lowercase() == name.lowercase() }
        }
    }
}