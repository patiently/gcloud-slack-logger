package io.patiently.gcloud.pubsub.kodein

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.patiently.clients.slack.SlackClient
import io.patiently.clients.slack.SlackConfig
import io.patiently.clients.victorops.GoAlertClient
import io.patiently.clients.victorops.GoAlertConfig
import kotlin.system.exitProcess
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.singleton

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
    bind<GoAlertConfig>() with factory {
        object : GoAlertConfig {
            override val token: String
                get() = System.getenv("GO_ALERT_TOKEN")
                    ?: exitProcess(1)

        }
    }
    bind<SlackClient>() with singleton { SlackClient(di) }
    bind<GoAlertClient>() with singleton { GoAlertClient(di) }
    bind<Gson>() with singleton {
        GsonBuilder()
            .create()
    }
}