package io.patiently.clients.slack

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.logging.Logger

class SlackClient(override val di: DI) : DIAware {

    private val config: SlackConfig by instance()

    private val slackService by lazy {
        val retrofit = Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer ${config.apiKey}")
                                .addHeader("Content-type", "application/json")
                                .build()
                        )
                    }
                    .build()
            )
            .baseUrl("https://slack.com/api/")
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder()
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .create()
                )
            )
            .build()
        retrofit.create(SlackService::class.java)
    }

    fun sendMessage(message: SlackMessage) {
        val call = slackService.postMessage(message)
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
        private val LOGGER = Logger.getLogger(SlackClient::class.java.name)
    }
}