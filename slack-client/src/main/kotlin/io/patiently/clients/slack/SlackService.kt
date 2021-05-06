package io.patiently.clients.slack

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface SlackService {

    @POST("chat.postMessage")
    fun postMessage(@Body message: SlackMessage): Call<ResponseBody>
}

data class SlackMessage(
    val channel: String,
    val text: String?,
    val attachments: List<SlackMessageAttachment>? = null,
    val blocks: List<SlackMessageBlock>? = null,
    val iconEmoji: String? = null,
    val iconUrl: String? = null,
    val linkNames: Boolean? = null,
    @SerializedName("mrkdwn")
    val enableMarkDown: Boolean? = null,
    val parse: SlackMessageParseType? = null,
    val replyBroadcast: Boolean? = null,
    val threadTs: String? = null,
    val unfurlLinks: Boolean? = null,
    val unfurlMedia: Boolean? = null,
    val userName: String? = null
)

data class SlackMessageAttachment(
    val fallback: String,
    val text: String?,
    val pretext: String?,
    val color: String?,
    val fields: List<Field>?,
    @SerializedName("mrkdwn_in")
    val useMarkdownIn: Set<String>? = null,
)

data class Field(
    var title: String,
    var value: String?,
    @SerializedName("short")
    var shortValue: Boolean?
)

data class SlackMessageBlock(
    val type: SlackMessageBlockType,
    val text: SlackMessageBlockText
)

enum class SlackMessageBlockType {
    @SerializedName("section")
    SECTION
}

data class SlackMessageBlockText(
    val type: SlackMessageBlockTextType,
    val text: String
)

enum class SlackMessageBlockTextType {
    @SerializedName("plain_text")
    PLAIN_TEXT,
    @SerializedName("mrkdwn")
    MARK_DOWN
}

enum class SlackMessageParseType {
    @SerializedName("none")
    NONE,

    @SerializedName("full")
    FULL
}