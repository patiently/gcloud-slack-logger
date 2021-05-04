package io.patiently.clients.slack

interface SlackConfig {
    val apiKey: String
    val slackChannel: String
}