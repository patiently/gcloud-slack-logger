import io.patiently.gcloud.pubsub.PubSubEventListener
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class NotifierTest {

    private lateinit var pubSubEventListenerListener: PubSubEventListener

    @BeforeAll
    fun setup() {
        pubSubEventListenerListener = PubSubEventListener()
    }

    @Test
    fun testSendSlackNotification() {
        fail("FOO BAR")
    }

    @Test
    fun testSendSlackAndVictorNotification() {
        fail("FOO BAR")
    }
}