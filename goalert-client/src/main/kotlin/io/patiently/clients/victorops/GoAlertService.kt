package io.patiently.clients.victorops

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// https://goalert.youcruit.com/api/v2/generic/incoming?token=

interface GoAlertService {
    @POST("api/v2/generic/incoming")
    fun triggerAlert(
        @Query("token")
        token: String,
        @Body
        message: GoAlertMessage
    ): Call<ResponseBody>
}

data class GoAlertMessage(
    @SerializedName("action")
    val action: Action,
    @SerializedName("details")
    val details: String,
    @SerializedName("summary")
    val summary: String,
    @SerializedName("dedupe")
    val dedupe: String?,
)

enum class Action {
    CLOSE,
    DOWN,
}