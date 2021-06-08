package io.patiently.gcloud.pubsub.kodein

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.patiently.clients.slack.SlackClient
import io.patiently.clients.slack.SlackConfig
import io.patiently.clients.victorops.VictorOpsClient
import io.patiently.clients.victorops.VictorOpsConfig
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.singleton
import kotlin.system.exitProcess

val cloudFunctionModule = DI.Module(name = "cloudFunctionModule") {
    bind<SlackConfig>() with factory {
        object : SlackConfig {
            override val apiKey: String
                get() = System.getenv("SLACK_API_KEY")
                    ?: exitProcess(1)

            override val slackChannel: String
                get() = System.getenv("SLACK_CHANNEL")
                    ?: exitProcess(1)

            override val kubeProjectIds: List<String>
                get() = System.getenv("KUBE_PROJECT_IDS")
                    ?.split(",")
                    ?.map {
                        it.trim()
                    }
                    ?: exitProcess(1)
        }
    }
    bind<VictorOpsConfig>() with factory {
        object : VictorOpsConfig {
            override val accountId: String
                get() = System.getenv("VICTOR_OPS_ACCOUNT_ID")
                    ?: exitProcess(1)

            override val routingKey: String
                get() = System.getenv("VICTOR_OPS_ROUTING_KEY")
                    ?: exitProcess(1)

            override val secretKey: String
                get() = System.getenv("VICTOR_OPS_SECRET_KEY")
                    ?: exitProcess(1)
        }
    }
    bind<SlackClient>() with singleton { SlackClient(di) }
    bind<VictorOpsClient>() with singleton { VictorOpsClient(di) }
    bind<Gson>() with singleton {
        GsonBuilder()
            .create()
    }
}