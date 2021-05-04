package io.patiently.clients.victorops

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface VictorOpsService {
    @POST("/integrations/generic/{accountId}/alert/{secretKey}/{routingKey}")
    fun triggerAlert(
        @Path("accountId")
        accountId: String,
        @Path("secretKey")
        secretKey: String,
        @Path("routingKey")
        routingKey: String,
        @Body
        message: VictorOpsMessage
    )
}

data class VictorOpsMessage(
    @SerializedName("message_type")
    val messageType: MessageType,
    @SerializedName("entity_id")
    val entityId: String,
    @SerializedName("entity_display_name")
    val entityDisplayName: String,
    @SerializedName("state_message")
    val stateMessage: String,
    // UNIX Time
    @SerializedName("state_start_time")
    val stateStartTime: Long,
    @SerializedName("vo_annotate.s.Note")
    val annotation: String
)

enum class MessageType {
    CRITICAL,
    WARNING,
    ACKNOWLEDGEMENT,
    INFO,
    RECOVERY
}