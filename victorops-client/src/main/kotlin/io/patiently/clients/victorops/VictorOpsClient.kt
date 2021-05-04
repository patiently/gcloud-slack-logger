package io.patiently.clients.victorops

import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import retrofit2.Retrofit

class VictorOpsClient(override val di: DI) : DIAware {

    private val config: VictorOpsConfig by instance()

    private val victorOpsService by lazy {
        val retrofit = Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                    .build()
            )
            .baseUrl("https://slack.com/api")
            .build()
        retrofit.create(VictorOpsService::class.java)
    }

    fun sendMessage(message: VictorOpsMessage) {
        victorOpsService.triggerAlert(
            accountId = config.accountId,
            secretKey = config.secretKey,
            routingKey = config.routingKey,
            message = message
        )
    }
}