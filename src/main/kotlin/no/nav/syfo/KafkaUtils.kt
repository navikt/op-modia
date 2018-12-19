package no.nav.syfo

import java.util.Properties

fun readConsumerConfig(
    env: Environment
) = Properties().apply {
    load(Environment::class.java.getResourceAsStream("/kafka_consumer.properties"))
    this["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"${env.srvappnameUsername}\" password=\"${env.srvappnamePassword}\";"
    this["bootstrap.servers"] = env.kafkaBootstrapServersURL
}
