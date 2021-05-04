package io.patiently.clients.victorops

interface VictorOpsConfig {
    val accountId: String
    val secretKey: String
    val routingKey: String
}