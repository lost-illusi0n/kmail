package dev.sitar.kmail.smtp.frames.replies

import kotlin.reflect.KClass


// TODO: clean up categories
public sealed interface SmtpReply<S: Any> {
    public val code: Int
    public val lines: List<String>

    public sealed interface PositiveCompletion : SmtpReply<PositiveCompletion> {
        public data class Default(
            override val code: Int,
            override val lines: List<String> = emptyList(),
        ) : PositiveCompletion

        public override fun <T : PositiveCompletion> tryAs(klass: KClass<T>): T? {
            return when (klass) {
                EhloCompletion::class -> {
                    if (code != 250) return null
                    return EhloCompletion.from(lines) as T
                }

                OkCompletion::class -> {
                    if (code != 250) return null
                    return OkCompletion.from(lines) as T
                }

                GreetCompletion::class -> {
                    if (code != 220) return null
                    return GreetCompletion.from(lines) as T
                }

                ReadyToStartTlsCompletion::class -> {
                    if (code != 220) return null
                    return ReadyToStartTlsCompletion.from(lines) as T
                }

                else -> error("$klass is not specialized!")
            }
        }

        public companion object {
            public const val DIGIT: Int = 2
        }
    }

    public sealed interface PositiveIntermediate : SmtpReply<PositiveIntermediate> {
        public data class Default(
            override val code: Int,
            override val lines: List<String> = emptyList(),
        ) : PositiveIntermediate

        public override fun <T : PositiveIntermediate> tryAs(klass: KClass<T>): T? {
            return when (klass) {
                StartMailInputIntermediary::class -> {
                    if (code != 354) return null
                    return StartMailInputIntermediary.from(lines) as T
                }
                else -> error("$klass is not specialized!")
            }
        }

        public companion object {
            public const val DIGIT: Int = 3
        }
    }
    public sealed interface TransientNegative : SmtpReply<TransientNegative> {
        public data class Default(
            override val code: Int,
            override val lines: List<String> = emptyList(),
        ) : TransientNegative

        override fun <T : TransientNegative> tryAs(klass: KClass<T>): T? {
            return null
        }

        public companion object {
            public const val DIGIT: Int = 4
        }
    }
    public sealed interface PermanentNegative : SmtpReply<PermanentNegative> {
        public data class Default(
            override val code: Int,
            override val lines: List<String> = emptyList(),
        ) : PermanentNegative

        override fun <T : PermanentNegative> tryAs(klass: KClass<T>): T? {
            return null
        }

        public companion object {
            public val DIGIT: Int = 5
        }
    }

    public fun <T: S> tryAs(klass: KClass<T>): T?
}

public inline fun <reified B: SmtpReply<B>, reified T: B> B.tryAs(): T? {
    return tryAs(T::class)
}