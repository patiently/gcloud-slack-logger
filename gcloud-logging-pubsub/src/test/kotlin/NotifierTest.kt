import com.google.cloud.functions.Context
import com.google.gson.GsonBuilder
import io.patiently.gcloud.pubsub.PubSubEventListener
import io.patiently.gcloud.pubsub.obj.LogSeverity
import io.patiently.gcloud.pubsub.obj.PubSubMessage
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.Base64

class NotifierTest {

    private val pubSubEventListenerListener = PubSubEventListener()
    private val gsonParser = GsonBuilder().create()

    @Test
    fun testSendSlackNotification() {
        pubSubEventListenerListener.accept(
            payload = generatePubSubMessage(severity = LogSeverity.WARNING),
            context = object : Context {
                override fun eventId(): String {
                    TODO("Not yet implemented")
                }

                override fun timestamp(): String {
                    TODO("Not yet implemented")
                }

                override fun eventType(): String {
                    TODO("Not yet implemented")
                }

                override fun resource(): String {
                    TODO("Not yet implemented")
                }
            }
        )
    }

    @Test
    fun testSendSlackAndVictorNotification() {
        fail("FOO BAR")
    }

    private fun generatePubSubMessage(severity: LogSeverity): PubSubMessage =
        PubSubMessage(
            data = generateB64LogEntry(severity),
        )

    private fun generateB64LogEntry(severity: LogSeverity, withJsonPayload: Boolean = false, withException: Boolean = false): String {

        @Language("JSON")
        val logInfo = if (withJsonPayload) {
            val exception = if (withException) {
                """
                   "org.opentest4j.AssertionFailedError: FOO BAR
                    at org.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:43)
                    at org.junit.jupiter.api.Assertions.fail(Assertions.java:129)
                    at org.junit.jupiter.api.AssertionsKt.fail(Assertions.kt:24)
                    ",
                """.trimIndent()
            } else {
                "\"\""
            }

            """
            "jsonPayload": {
                "level": "error",
                "timestamp": "2021-05-04T19:47:43.842Z",
                "message": "grpc.handleStatusCode, code: 2, details: Response closed without grpc-status (Headers only)",
                "exception": $exception
              },
            """.trimIndent()
        } else {
            """
            "textPayload": "grpc.handleStatusCode, code: 2, details: Response closed without grpc-status (Headers only)",
            """.trimIndent()
        }

        @Language("JSON")
        val json =
            """
            {
              "insertId": "1cugvltg3nishwm",
              $logInfo
              "resource": {
                "type": "k8s_container",
                "labels": {
                  "namespace_name": "default",
                  "pod_name": "micro-chat-4a01383-77659b677c-x5v9n",
                  "cluster_name": "youcruit-us",
                  "project_id": "youcruit",
                  "location": "us-east4",
                  "container_name": "micro-chat-webserver"
                }
              },
              "timestamp": "2021-05-04T19:47:43.842Z",
              "severity": "${severity.name}",
              "labels": {
                "k8s-pod/hz": "micro-chat",
                "k8s-pod/pod-template-hash": "77659b677c",
                "k8s-pod/commitId": "4a01383",
                "compute.googleapis.com/resource_name": "gke-youcruit-us-default-pool-a683b8d3-zht5",
                "k8s-pod/app": "micro-chat",
                "k8s-pod/version": "1.0.27"
              },
              "logName": "projects/youcruit/logs/stdout",
              "receiveTimestamp": "2021-05-04T19:47:44.020339350Z"
            }
            """.trimIndent()
        return Base64.getEncoder().encodeToString(json.toByteArray())
    }
}