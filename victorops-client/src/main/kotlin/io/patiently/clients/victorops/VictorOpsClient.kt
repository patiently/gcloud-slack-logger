package io.patiently.clients.victorops

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.logging.Logger

class VictorOpsClient(override val di: DI) : DIAware {

    private val config: VictorOpsConfig by instance()

    private val victorOpsService by lazy {
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
            .baseUrl("https://alert.victorops.com/")
            .build()
        retrofit.create(VictorOpsService::class.java)
    }

    fun sendMessage(message: VictorOpsMessage) {
        val call = victorOpsService.triggerAlert(
            accountId = config.accountId,
            secretKey = config.secretKey,
            routingKey = config.routingKey,
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
        private val LOGGER = Logger.getLogger(VictorOpsClient::class.java.name)
    }
}