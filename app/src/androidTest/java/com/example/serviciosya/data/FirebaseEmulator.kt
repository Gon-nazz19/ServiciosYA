package com.example.serviciosya.data

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import java.net.HttpURLConnection
import java.net.URL

/**
 * Connects to the local Firebase emulators (Firestore :8080, Auth :9099) through a separate
 * FirebaseApp with a "demo-" project id, so these tests can never reach the real project.
 */
object FirebaseEmulator {
    const val PROJECT_ID = "demo-serviciosya"
    private const val HOST = "10.0.2.2" // Host machine as seen from the Android emulator.
    private const val FIRESTORE_PORT = 8080
    private const val AUTH_PORT = 9099
    private const val APP_NAME = "emulator-tests"

    val app: FirebaseApp by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(PROJECT_ID)
                    .setApplicationId("1:000000000000:android:0000000000000000")
                    .setApiKey("fake-api-key-for-emulator")
                    .build(),
                APP_NAME,
            )
    }

    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance(app).apply { useEmulator(HOST, AUTH_PORT) }
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(app).apply {
            useEmulator(HOST, FIRESTORE_PORT)
            firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings { }) }
        }
    }

    /** Deletes every document and every user in the emulators. */
    fun reset() {
        auth.signOut()
        request("DELETE", "http://$HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID/databases/(default)/documents")
        request("DELETE", "http://$HOST:$AUTH_PORT/emulator/v1/projects/$PROJECT_ID/accounts")
    }

    /**
     * Writes a document bypassing security rules ("Bearer owner" is accepted only by the emulator),
     * the same way the seed script loads catalog data with the Admin SDK.
     */
    fun seed(path: String, fields: Map<String, Any?>) {
        val collection = path.substringBeforeLast('/')
        val documentId = path.substringAfterLast('/')
        val body = fields.entries.joinToString(prefix = """{"fields":{""", postfix = "}}") { (key, value) ->
            "\"$key\":${value.toFirestoreValue()}"
        }
        request(
            method = "POST",
            url = "http://$HOST:$FIRESTORE_PORT/v1/projects/$PROJECT_ID/databases/(default)/documents/" +
                "$collection?documentId=$documentId",
            body = body,
        )
    }

    private fun Any?.toFirestoreValue(): String = when (this) {
        null -> """{"nullValue":null}"""
        is Boolean -> """{"booleanValue":$this}"""
        is Int, is Long -> """{"integerValue":"$this"}"""
        is Double -> """{"doubleValue":$this}"""
        is String -> """{"stringValue":"${replace("\\", "\\\\").replace("\"", "\\\"")}"}"""
        else -> error("Unsupported seed value: $this")
    }

    private fun request(method: String, url: String, body: String? = null) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.setRequestProperty("Authorization", "Bearer owner")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            check(code in 200..299) {
                "Emulator request $method $url failed ($code). Are the Firebase emulators running? " +
                    connection.errorStream?.bufferedReader()?.readText()
            }
        } finally {
            connection.disconnect()
        }
    }
}
