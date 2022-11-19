package dev.sitar.kmail.smtp.frames.replies

import dev.sitar.kmail.smtp.io.smtp.reader.AsyncSmtpReader
import kotlin.reflect.KClass


// TODO: clean up categories
public sealed interface SmtpReply<S: Any> {
    public val code: Int
    public val data: String?
    public val lines: List<String>

    public sealed interface PositiveCompletion : SmtpReply<PositiveCompletion> {
        public data class Default(
            override val code: Int,
            override val data: String?,
            override val lines: List<String> = emptyList(),
        ) : PositiveCompletion

        public override fun <T : PositiveCompletion> tryAs(klass: KClass<T>): T? {
            return when (klass) {
                EhloCompletion::class -> {
                    if (code != 250) return null
                    return EhloCompletion.from(data!!, lines) as T
                }

                OkCompletion::class -> {
                    if (code != 250) return null
                    return OkCompletion.from(data!!) as T
                }

                GreetCompletion::class -> {
                    if (code != 220) return null
                    return GreetCompletion.from(data!!) as T
                }

                ReadyToStartTlsCompletion::class -> {
                    if (code != 220) return null
                    return ReadyToStartTlsCompletion.from(data!!) as T
                }

                else -> error("$klass is not specialized!")
            }
        }

        public companion object {
            public const val DIGIT: Int = 2

            public suspend fun deserialize(code: Int, input: AsyncSmtpReader): Default {
                val lines = mutableListOf<String>()

                var isFinal = input.readIsFinal()

                val data = input.readUtf8UntilSmtpEnding()

                while (!isFinal) {
                    input.discard(3) // the code. we dont care
                    isFinal = input.readIsFinal()
                    lines += input.readUtf8UntilSmtpEnding()
                }

                return Default(code, data, lines)
            }
        }
    }

    public sealed interface PositiveIntermediate : SmtpReply<PositiveIntermediate> {
        public data class Default(
            override val code: Int,
            override val data: String?,
            override val lines: List<String> = emptyList(),
        ) : PositiveIntermediate

        public override fun <T : PositiveIntermediate> tryAs(klass: KClass<T>): T? {
            return when (klass) {
                StartMailInputIntermediary::class -> {
                    if (code != 354) return null
                    return StartMailInputIntermediary.from(data!!) as T
                }
                else -> error("$klass is not specialized!")
            }
        }

        public companion object {
            public const val DIGIT: Int = 3

            public suspend fun deserialize(code: Int, input: AsyncSmtpReader): Default {
                val lines = mutableListOf<String>()

                var isFinal = input.readIsFinal()

                val data = input.readUtf8UntilSmtpEnding()

                while (!isFinal) {
                    isFinal = input.readIsFinal()
                    lines += input.readUtf8UntilSmtpEnding()
                }

                return Default(code, data, lines)
            }
        }
    }
    public sealed interface TransientNegative : SmtpReply<TransientNegative> {
        public data class Default(
            override val code: Int,
            override val data: String?,
            override val lines: List<String> = emptyList(),
        ) : TransientNegative

        override fun <T : TransientNegative> tryAs(klass: KClass<T>): T? {
            return null
        }

        public companion object {
            public const val DIGIT: Int = 4

            public suspend fun deserialize(code: Int, input: AsyncSmtpReader): Default {
                val lines = mutableListOf<String>()

                var isFinal = input.readIsFinal()

                val data = input.readUtf8UntilSmtpEnding()

                while (!isFinal) {
                    isFinal = input.readIsFinal()
                    lines += input.readUtf8UntilSmtpEnding()
                }

                return Default(code, data, lines)
            }
        }
    }
    public sealed interface PermanentNegative : SmtpReply<PermanentNegative> {
        public data class Default(
            override val code: Int,
            override val data: String?,
            override val lines: List<String> = emptyList(),
        ) : PermanentNegative

        override fun <T : PermanentNegative> tryAs(klass: KClass<T>): T? {
            return null
        }

        public companion object {
            public val DIGIT: Int = 5

            public suspend fun deserialize(code: Int, input: AsyncSmtpReader): Default {
                val lines = mutableListOf<String>()

                var isFinal = input.readIsFinal()

                val data = input.readUtf8UntilSmtpEnding()

                while (!isFinal) {
                    isFinal = input.readIsFinal()
                    lines += input.readUtf8UntilSmtpEnding()
                }

                return Default(code, data, lines)
            }
        }
    }

    public fun <T: S> tryAs(klass: KClass<T>): T?
}

public inline fun <reified B: SmtpReply<B>, reified T: B> B.tryAs(): T? {
    return tryAs(T::class)
}