package io.patiently.gcloud.pubsub

import com.google.cloud.functions.BackgroundFunction
import com.google.cloud.functions.Context
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
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
import org.xbill.DNS.Lookup
import org.xbill.DNS.ReverseMap
import org.xbill.DNS.Type
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
    private val dnsCache: Cache<String, String> = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .build()

    override fun accept(payload: PubSubMessage?, context: Context) {
        try {
            if (payload?.data != null) {
                val data = String(Base64.getDecoder().decode(payload.data))
                val logEntry = pubSubMessageMapper.fromJson(data, LogEntry::class.java)
                processLogMessage(logEntry)
            } else {
                LOGGER.info { "Got a pubsub message without body" }
            }
        } catch (e: Exception) {
            LOGGER.severe {
                "${e.message}\n\n${e.stackTraceToString()}"
            }
        }
    }

    private fun processLogMessage(logEntry: LogEntry) {
        slackClient.sendMessage(generateSlackMessage(logEntry))
        if (logEntry.severity == LogSeverity.ALERT) {
            victorOpsClient.sendMessage(generateVictorOpsMessage(logEntry))
        }
    }

    private fun getDnsName(ipAddress: String?): String? {
        if (ipAddress == null || ipAddress == "N/A") {
            return null
        }

        val cachedDnsName = dnsCache.getIfPresent(ipAddress)
        return if (cachedDnsName != null) {
            cachedDnsName
        } else {
            val lookupDns = Lookup(ReverseMap.fromAddress(ipAddress), Type.PTR)
                .run()
                ?.firstOrNull()
                ?.rdataToString()
                ?.removeSuffix(".")

            lookupDns?.let {
                dnsCache.get(ipAddress) {
                    it
                }
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
        val cluster = logEntry.resource.labels?.get("cluster_name")
            ?: "N/A"
        val shortMessage = (
            logEntry.jsonPayload?.get("message")?.asString
                ?: logEntry.textPayload
            )
            ?.take(160)
            ?: ""
        val instance = logEntry.resource.labels?.get("container_name")
            ?: "N/A"
        val notificationMessage = "$cluster -> $instance\n $shortMessage"
        val consoleTraceLink = logEntry.trace?.split("/")?.last()
            ?.let {
                "https://console.cloud.google.com/traces/list?project=$projectId&tid=$it"
            }
        val cloudConsoleLink =
            "https://console.cloud.google.com/logs/query;query=insertId%3D%22${logEntry.insertId}%22;timeRange=P7D?project=$projectId"

        val now = Instant.now()
        val fromTime = now.minusMillis(5)
        val toTime = now.plusMillis(5)

        val links = slackConfig.kubeProjectIds.map { pId ->
            "$pId»https://console.cloud.google.com/logs/query;query=;timeRange=$fromTime%2F$toTime;cursorTimestamp=$now?project=$pId"
        }

        return SlackMessage(
            text = notificationMessage,
            enableMarkDown = true,
            channel = slackConfig.slackChannel,
            iconEmoji = getEmoji(logEntry),
            blocks = mutableListOf(
                SlackMessageBlock(
                    type = SlackMessageBlockType.SECTION,
                    text = SlackMessageBlockText(
                        type = SlackMessageBlockTextType.MARK_DOWN,
                        text = if (consoleTraceLink != null) {
                            "<$cloudConsoleLink|Show log in cloud console> / <$consoleTraceLink|Show trace in cloud console>"
                        } else {
                            "<$cloudConsoleLink|Show log in cloud console>"
                        }
                    )
                ),
                SlackMessageBlock(
                    type = SlackMessageBlockType.SECTION,
                    text = SlackMessageBlockText(
                        type = SlackMessageBlockTextType.MARK_DOWN,
                        text = links.joinToString(separator = " / ") {
                            val (project, link) = it.split("»")
                            "<$link| +/- 5 sec for $project>"
                        }
                    )
                ),
                SlackMessageBlock(
                    type = SlackMessageBlockType.SECTION,
                    text = SlackMessageBlockText(
                        type = SlackMessageBlockTextType.MARK_DOWN,
                        text = (
                            message
                                ?: textPayload
                                ?: "No message available for this entry?"
                            )
                            .let {
                                "```$it```"
                            }
                    )
                )
            ),
            attachments = mutableListOf(
                SlackMessageAttachment(
                    fallback = "",
                    color = color,
                    fields = generateFields(logEntry),
                    text = null,
                    pretext = null,
                )
            ).also { list ->
                exception?.let {
                    if (it.isNotBlank()) {
                        list.add(
                            SlackMessageAttachment(
                                fallback = "",
                                color = color,
                                useMarkdownIn = setOf("text"),
                                text = "*Exception*\n\n```$it```",
                                pretext = null,
                                fields = null,
                            )
                        )
                    }
                }
            }
        )
    }

    private fun generateFields(logEntry: LogEntry): List<Field> {
        val clusterName = logEntry.resource.labels?.get("cluster_name")
        val project = logEntry.resource.labels?.get("project_id")
        val containerName = logEntry.resource.labels?.get("container_name")

        val ipAddress = logEntry.jsonPayload?.get("remoteIp")?.asString
        val remoteHost = getDnsName(ipAddress)

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
        clusterName?.let {
            fields.add(
                Field(
                    title = "Cluster",
                    value = it,
                    shortValue = true
                )
            )
        }
        containerName?.let {
            fields.add(
                Field(
                    title = "Container",
                    value = it,
                    shortValue = true
                )
            )
        }
        project?.let {
            fields.add(
                Field(
                    title = "Project",
                    value = it,
                    shortValue = true
                )
            )
        }
        ipAddress?.let {
            fields.add(
                Field(
                    title = "Remote Host",
                    value = if (remoteHost != null) {
                        "$it ($remoteHost)"
                    } else {
                        it
                    },
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
        LogSeverity.WARNING -> "#DAA520"
        LogSeverity.ERROR -> "#8e2300"
        LogSeverity.ALERT -> "#cc3300"
        null -> "#cc3300"
    }

    companion object {
        private val LOGGER = Logger.getLogger(PubSubEventListener::class.java.name)
    }
}