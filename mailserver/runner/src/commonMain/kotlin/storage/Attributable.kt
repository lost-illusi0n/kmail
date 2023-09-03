package dev.sitar.kmail.runner.storage

interface Attributable {
    val attributes: Attributes
}

interface Attributes {
    suspend fun get(name: String): String?

    suspend fun set(name: String, value: String)

    suspend fun append(name: String, value: String): String {
        val new = get(name) + "$value\n"
        set(name, new)
        return new
    }
}