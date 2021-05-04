package io.patiently.gcloud.pubsub.obj

data class PubSubMessage(
    val data: String? = null,
    val attributes: Map<String, String>? = null,
    val messageId: String? = null,
    val publishTime: String? = null
)