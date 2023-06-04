package dev.sitar.kmail.agents.smtp.transfer

import dev.sitar.kmail.agents.smtp.queueId
import dev.sitar.kmail.smtp.InternetMessage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val logger = KotlinLogging.logger { }

class TransferServer(
    private val config: TransferConfig,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) {
    private val scope = CoroutineScope(CoroutineName("smtp-transfer-server") + SupervisorJob() + coroutineContext)

    fun handle(outgoingMail: Flow<InternetMessage>) {
        scope.launch {
            outgoingMail.collect {
                logger.info { "Beginning transfer of ${it.queueId}: $it" }

                launch {
                    TransferSendAgent(config, it).transfer()
                }
            }
        }
    }
}