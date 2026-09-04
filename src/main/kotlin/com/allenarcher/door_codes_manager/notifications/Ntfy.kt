package com.allenarcher.door_codes_manager.notifications

import jakarta.annotation.PostConstruct
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

@Component
class Ntfy(
    @Value($$"${ntfy.enabled:false}") private val enabled: Boolean,
    @Value($$"${ntfy.url:}") private val url: String,
    @Value($$"${ntfy.topic:}") private val topic: String,
    @Value($$"${ntfy.user:}") private val user: String?,
    @Value($$"${ntfy.password:}") private val password: String?,
    @Value($$"${ntfy.token:}") private val token: String?,
) {

    private val logger: Logger = LogManager.getLogger()!!
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private var auth: String? = null

    @PostConstruct
    fun postConstruct() {
        var message = "Ntfy notifications are enabled at $url"
        if (user?.isNotBlank() == true && password?.isNotBlank() == true) {
            val encoded = Base64.getEncoder().encodeToString("$user:$password".toByteArray())
            auth = "Basic $encoded"
            message += " using basic auth"
        } else if (token?.isNotBlank() == true) {
            auth = "Bearer $token"
            message += " using token auth"
        } else {
            message += " with no auth"
        }
        if (enabled) {
            logger.info(message)
        } else {
            logger.info("Ntfy notifications disabled")
        }
    }

    fun sendNotification(title: String, message: String, priority: String, tags: String) {
        if (!enabled) return
        val requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("$url/$topic"))
            .POST(HttpRequest.BodyPublishers.ofString(message))
            .header("Title", title)
            .header("Priority", priority)
            .header("Tags", tags)
        auth?.let { requestBuilder.header("Authorization", it) }
        try {
            val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() != 200) {
                logger.error("Non-successful response from ntfy request: ${response.statusCode()}")
            }
        } catch (e: Exception) {
            logger.error("Error sending request to ntfy at $url: $e")
        }
    }
}