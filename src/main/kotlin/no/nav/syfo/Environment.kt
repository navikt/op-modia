package no.nav.syfo

import java.util.Properties

fun getEnvVar(name: String, default: String? = null): String =
        System.getenv(name) ?: default ?: throw RuntimeException("Missing variable $name")

// TODO removed when added in vault
// private val vaultApplicationPropertiesPath = Paths.get("/var/run/secrets/nais.io/vault/application.properties")

private val config = Properties().apply {
    putAll(Properties().apply {
        load(Environment::class.java.getResourceAsStream("/application.properties"))
    })
    /*
    if (Files.exists(vaultApplicationPropertiesPath)) {
        load(Files.newInputStream(vaultApplicationPropertiesPath))
    }
    */
}
data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationThreads: Int = getEnvVar("APPLICATION_THREADS", "4").toInt(),
    val srvappnameUsername: String = getEnvVar("SRVOP-MODIA_USERNAME"),
    val srvappnamePassword: String = getEnvVar("SRVOP-MODIA_PASSWORD"),
    val kafkaBootstrapServersURL: String = getEnvVar("KAFKA_BOOTSTRAP_SERVERS_URL"),

    val kafkaIncommingTopicOppfolginsplan: String = config.getProperty("kafka.privat.syfo.oppfolingsplan.topic")
)
