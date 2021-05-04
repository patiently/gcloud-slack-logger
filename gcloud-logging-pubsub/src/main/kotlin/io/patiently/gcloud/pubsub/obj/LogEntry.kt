package io.patiently.gcloud.pubsub.obj

import com.google.gson.JsonObject

data class LogEntry(
    val logName: String?,
    val resource: MonitoredResource,
    val timestamp: String?,
    val receivedTimestamp: String?,
    val severity: LogSeverity?,
    val insertId: String?,
    val httpRequest: HttpRequest?,
    val labels: Map<String, String>?,
    val metadata: MonitoredResourceMetadata?,
    val operation: LogEntryOperation?,
    val trace: String?,
    val traceSampled: Boolean?,
    val jsonPayload: JsonObject?,
    val textPayload: String?
)
/*
k8s-pod/version: "0.1.71-SNAPSHOT"
k8s-pod/commitId: "3b2441a"
k8s-pod/pod-template-hash: "7bc8574876"
k8s-pod/app: "lanefinder-graphql-server"
compute.googleapis.com/resource_name: "gke-yobify-staging-yobify-staging-poo-67ee536f-fu10"
k8s-pod/hz: "lanefinder-graphql-server"
 */

data class HttpRequest(
    val requestMethod: String?,
    val requestUrl: String?,
    val requestSize: String?,
    val status: Int?,
    val responseSize: String?,
    val userAgent: String?,
    val remoteIp: String?,
    val serverIp: String?,
    val referer: String?,
    val latency: String?,
    val cacheLookup: Boolean?,
    val cacheHit: Boolean?,
    val cacheValidatedWithOriginServer: Boolean?,
    val cacheFillBytes: String?,
    val protocol: String?
)

data class MonitoredResource(
    val type: String?,
    val labels: Map<String, String>?
)

data class MonitoredResourceMetadata(
    val systemLabels: JsonObject?,
    val userLabels: Map<String, String?>
)

data class LogEntryOperation(
    val id: String?,
    val producer: String?,
    val first: Boolean?,
    val last: Boolean?

)

enum class LogSeverity {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    NOTICE,
    ALERT
}
