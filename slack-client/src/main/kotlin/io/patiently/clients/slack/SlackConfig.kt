package io.patiently.clients.slack

interface SlackConfig {
    val apiKey: String
    val slackChannel: String
    // Used for fetching all logs surrounding the current error on all projects
    val kubeProjectIds: List<String>
}