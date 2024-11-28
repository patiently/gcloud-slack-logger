package io.patiently.clients.victorops

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.logging.Logger

class GoAlertClient(override val di: DI) : DIAware {

    private val config: GoAlertConfig by instance()

    private val goAlertService by lazy {
        val retrofit = Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                    .build()
            )
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .create()
                )
            )
            .baseUrl("https://goalert.youcruit.com/")
            .build()
        retrofit.create(GoAlertService::class.java)
    }

    fun sendMessage(message: GoAlertMessage) {
        val call = goAlertService.triggerAlert(
            token = config.token,
            message = message
        )
        val response = call.execute()
        if (!response.isSuccessful) {
            val errorResponse = response.errorBody()?.use {
                it.string()
            }
            LOGGER.severe {
                "Failed to send slack message: ${response.code()} $errorResponse"
            }
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(GoAlertService::class.java.name)
    }
}