package io.patiently.gcloud.pubsub

import com.google.cloud.functions.BackgroundFunction
import com.google.cloud.functions.Context
import com.google.gson.Gson
import io.patiently.clients.slack.Field
import io.patiently.clients.slack.SlackClient
import io.patiently.clients.slack.SlackConfig
import io.patiently.clients.slack.SlackMessage
import io.patiently.clients.slack.SlackMessageAttachment
import io.patiently.clients.slack.SlackMessageBlock
import io.patiently.clients.slack.SlackMessageBlockText
import io.patiently.clients.slack.SlackMessageBlockTextType
import io.patiently.clients.slack.SlackMessageBlockType
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
import java.util.logging.Logger

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
        try {
            slackClient.sendMessage(generateSlackMessage(logEntry))
            if (logEntry.severity == LogSeverity.ALERT) {
                victorOpsClient.sendMessage(generateVictorOpsMessage(logEntry))
            }
        } catch (e: Exception) {
            LOGGER.severe {
                "${e.message}\n\n${e.stackTraceToString()}"
            }
        }
    }

    private fun generateVictorOpsMessage(logEntry: LogEntry): VictorOpsMessage {
        val jsonPayload = logEntry.jsonPayload
        val displayName = jsonPayload?.get("message")?.asString
            ?: logEntry.textPayload
            ?: "No message data found"
        val stateMessage = jsonPayload?.get("exception")?.asString
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
        val color = getColor(logEntry)
        val exception = logEntry.jsonPayload?.get("exception")?.asString
        val message = logEntry.jsonPayload?.get("message")?.asString
        val textPayload = logEntry.textPayload
        val projectId = logEntry.resource.labels?.get("project_id")
        val consoleTraceLink = logEntry.trace?.split("/")?.last()
            ?.let {
                "https://console.cloud.google.com/traces/list?project=$projectId&tid=$it"
            }
        val cloudConsoleLink = "https://console.cloud.google.com/logs/query;query=insertId%3D%22${logEntry.insertId}%22;timeRange=P7D?project=$projectId"
        val slackMessage = SlackMessage(
            text = null,
            channel = slackConfig.slackChannel,
            iconEmoji = getEmoji(logEntry),
            blocks = mutableListOf(
                SlackMessageBlock(
                    type = SlackMessageBlockType.SECTION,
                    text = SlackMessageBlockText(
                        type = SlackMessageBlockTextType.MARK_DOWN,
                        text = if (consoleTraceLink != null) {
                            "<$cloudConsoleLink|Show log in cloud console> / <$consoleTraceLink|Show trace in cloude console>"
                        } else {
                            "<$cloudConsoleLink|Show log in cloud console>"
                        }
                    )
                )
            ),
            attachments = listOf(
                SlackMessageAttachment(
                    fallback = "",
                    color = color,
                    fields = generateFields(logEntry),
                    text = null,
                    pretext = null,
                ),
                exception?.let {
                    SlackMessageAttachment(
                        fallback = "",
                        color = color,
                        text = null,
                        pretext = null,
                        fields = mutableListOf(
                            Field(
                                title = "Exception",
                                value = it,
                                shortValue = false
                            )
                        ).also { list ->
                            message?.let { message ->
                                list.add(
                                    Field(
                                        title = "Message",
                                        value = message,
                                        shortValue = true
                                    )
                                )
                            }
                        }
                    )
                }
                    ?: message?.let {
                        SlackMessageAttachment(
                            fallback = "",
                            color = color,
                            pretext = null,
                            text = null,
                            fields = listOf(
                                Field(
                                    title = "Message",
                                    value = it,
                                    shortValue = true
                                )
                            )
                        )
                    }
                    ?: textPayload?.let {
                        SlackMessageAttachment(
                            fallback = "",
                            color = color,
                            pretext = null,
                            text = null,
                            fields = listOf(
                                Field(
                                    title = "Message",
                                    value = it,
                                    shortValue = true
                                )
                            )
                        )
                    }
                    ?: SlackMessageAttachment(
                        fallback = "",
                        color = color,
                        pretext = "No additional data",
                        text = "",
                        fields = null
                    )
            )
        )
        return slackMessage
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
        LogSeverity.INFO -> "#339900"
        LogSeverity.WARNING,
        LogSeverity.ERROR -> "#DAA520"
        LogSeverity.ALERT -> "#cc3300"
        null -> "#cc3300"
    }

    companion object {
        private val LOGGER = Logger.getLogger(PubSubEventListener::class.java.name)
    }
}