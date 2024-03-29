package no.nav.syfo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.api.registerNaisApi
import no.nav.syfo.model.ReceivedOppfolginsplan
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)
val log: Logger = LoggerFactory.getLogger("no.nav.syfo")
val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

fun main(args: Array<String>) = runBlocking(Executors.newFixedThreadPool(2).asCoroutineDispatcher()) {
    val env = Environment()
    val applicationState = ApplicationState()

    val applicationServer = embeddedServer(Netty, env.applicationPort) {
        initRouting(applicationState)
    }.start(wait = false)

    try {
        val listeners = (1..env.applicationThreads).map {
            launch {
                val consumerProperties = readConsumerConfig(env)

                val opConsumer = KafkaConsumer<String, String>(consumerProperties)
                opConsumer.subscribe(listOf(env.kafkaIncommingTopicOppfolginsplan))
                blockingApplicationLogic(applicationState, opConsumer)
            }
        }.toList()

        runBlocking {
            Runtime.getRuntime().addShutdownHook(Thread {
                applicationServer.stop(10, 10, TimeUnit.SECONDS)
            })

            applicationState.initialized = true
            listeners.forEach { it.join() }
        }
    } finally {
        applicationState.running = false
    }
}

suspend fun blockingApplicationLogic(applicationState: ApplicationState, opConsumer: KafkaConsumer<String, String>) {
    while (applicationState.running) {
        opConsumer.poll(Duration.ofMillis(0)).forEach {
            val receivedOppfolginsplan: ReceivedOppfolginsplan = objectMapper.readValue(it.value())
            val logValues = arrayOf(
                    StructuredArguments.keyValue("orgNr", receivedOppfolginsplan.senderOrgId),
                    StructuredArguments.keyValue("navkontor", receivedOppfolginsplan.navLogId)
            )
            val logKeys = logValues.joinToString(prefix = "(", postfix = ")", separator = ",") { "{}" }
            log.info("Received a Altinn oppfølgingsplan $logKeys", *logValues)
        }
        delay(100)
    }
}

fun Application.initRouting(applicationState: ApplicationState) {
    routing {
        registerNaisApi(
                readynessCheck = {
                    applicationState.initialized
                },
                livenessCheck = {
                    applicationState.running
                }
        )
    }
}
