package io.patiently.gcloud.pubsub

import com.google.cloud.functions.BackgroundFunction
import com.google.cloud.functions.Context
import com.google.gson.Gson
import io.patiently.clients.slack.Field
import io.patiently.clients.slack.SlackClient
import io.patiently.clients.slack.SlackConfig
import io.patiently.clients.slack.SlackMessage
import io.patiently.clients.slack.SlackMessageAttachment
import io.patiently.clients.victorops.MessageType
import io.patiently.clients.victorops.VictorOpsClient
import io.patiently.clients.victorops.VictorOpsMessage
import io.patiently.gcloud.pubsub.kodein.cloudFunctionModule
import io.patiently.gcloud.pubsub.obj.LogEntry
import io.patiently.gcloud.pubsub.obj.LogSeverity
import io.patiently.gcloud.pubsub.obj.PubSubMessage
import org.kodein.di.DI
import org.kodein.di.instance
import java.time.Instant
import java.util.Base64

class PubSubEventListener : BackgroundFunction<PubSubMessage> {

    private val di = DI.from(
        listOf(
            cloudFunctionModule
        )
    )

    private val slackClient: SlackClient by di.instance()
    private val slackConfig: SlackConfig by di.instance()
    private val victorOpsClient: VictorOpsClient by di.instance()
    private val pubSubMessageMapper: Gson by di.instance()

    override fun accept(payload: PubSubMessage, context: Context) {
        val data = String(Base64.getDecoder().decode(payload.data))
        val logEntry = pubSubMessageMapper.fromJson(data, LogEntry::class.java)
        processLogMessage(logEntry)
    }

    private fun processLogMessage(logEntry: LogEntry) {
        slackClient.sendMessage(generateSlackMessage(logEntry))
        if (logEntry.severity == LogSeverity.ALERT) {
            victorOpsClient.sendMessage(generateVictorOpsMessage(logEntry))
        }
    }

    private fun generateVictorOpsMessage(logEntry: LogEntry): VictorOpsMessage {
        val jsonPayload = logEntry.jsonPayload
        val displayName = jsonPayload?.getAsJsonObject("message")?.asString
            ?: logEntry.textPayload
            ?: "No message data found"
        val stateMessage = logEntry.jsonPayload?.getAsJsonObject("exception")?.asString
            ?: ""
        val stateStartTime = logEntry.receivedTimestamp?.let {
            Instant.parse(it)
        } ?: Instant.now()
        val clusterName = logEntry.resource.labels?.get("cluster_name") ?: "N/A"
        val project = logEntry.resource.labels?.get("project_id") ?: "N/A"
        val containerName = logEntry.resource.labels?.get("container_name") ?: "N/A"
        val annotation = "$clusterName -> $project -> $containerName"
        return VictorOpsMessage(
            messageType = MessageType.CRITICAL,
            entityId = logEntry.insertId
                ?: "No id found",
            entityDisplayName = displayName,
            stateMessage = stateMessage,
            stateStartTime = stateStartTime.epochSecond,
            annotation = annotation
        )
    }

    private fun generateSlackMessage(logEntry: LogEntry): SlackMessage {
        return SlackMessage(
            text = null,
            channel = slackConfig.slackChannel,
            iconEmoji = getEmoji(logEntry),
            attachments = listOf(
                SlackMessageAttachment(
                    fallback = "",
                    color = getColor(logEntry),
                    fields = generateFields(logEntry),
                    text = null,
                    pretext = null,
                ),
                logEntry.jsonPayload?.get("exception")?.asString?.let {
                    SlackMessageAttachment(
                        fallback = "",
                        color = null,
                        pretext = "Exception",
                        text = it,
                        fields = null
                    )
                }
                    ?: logEntry.jsonPayload?.get("message")?.asString?.let {
                        SlackMessageAttachment(
                            fallback = "",
                            color = null,
                            pretext = "Message",
                            text = it,
                            fields = null
                        )
                    }
                    ?: logEntry.textPayload?.let {
                        SlackMessageAttachment(
                            fallback = "",
                            color = null,
                            pretext = "Message",
                            text = it,
                            fields = null
                        )
                    }
                    ?: SlackMessageAttachment(
                        fallback = "",
                        color = null,
                        pretext = "No additional data",
                        text = "",
                        fields = null
                    )
            )
        )
    }

    private fun generateFields(logEntry: LogEntry): List<Field> {
        val fields = mutableListOf<Field>()
        logEntry.labels?.get("k8s-pod/app")?.let {
            fields.add(
                Field(
                    title = "App",
                    value = it,
                    shortValue = true
                )
            )
        }
        logEntry.labels?.get("k8s-pod/version")?.let {
            fields.add(
                Field(
                    title = "Version",
                    value = it,
                    shortValue = true
                )
            )
        }
        logEntry.labels?.get("k8s-pod/commitId")?.let {
            fields.add(
                Field(
                    title = "Commit",
                    value = it,
                    shortValue = true
                )
            )
        }
        return fields
    }

    private fun getEmoji(logEntry: LogEntry) = when (logEntry.severity) {
        LogSeverity.DEBUG -> ":pawprints:"
        LogSeverity.NOTICE -> ":beetle:"
        LogSeverity.INFO -> ":suspect:"
        LogSeverity.WARNING -> ":goberserk:"
        LogSeverity.ERROR -> ":feelsgood:"
        LogSeverity.ALERT -> ":finnadie:"
        null -> ":finnadie:"
    }

    private fun getColor(logEntry: LogEntry) = when (logEntry.severity) {
        LogSeverity.DEBUG,
        LogSeverity.NOTICE,
        LogSeverity.INFO -> "good"
        LogSeverity.WARNING,
        LogSeverity.ERROR -> "warning"
        LogSeverity.ALERT -> "error"
        null -> "error"
    }
}