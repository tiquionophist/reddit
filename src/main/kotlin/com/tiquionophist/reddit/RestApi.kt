package com.tiquionophist.reddit

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * A simple wrapper around a REST API.
 */
abstract class RestApi {

    /**
     * Headers that should be included by default in all requests made by this [RestApi]; default empty.
     *
     * Each key-value pair of this map will be included as a header name-value pair if the key is not blank and the
     * value is non-null and not blank.
     */
    protected open val headers: Map<String, String?> = emptyMap()

    protected val gson = Gson()
    protected val httpClient: HttpClient = HttpClient.newHttpClient()

    /**
     * Creates a GET [HttpRequest] to the given [url].
     *
     * @param includeHeaders whether to include [headers] in the returned [HttpRequest]; default true
     */
    protected fun buildGET(
        url: String,
        includeHeaders: Boolean = true
    ): HttpRequest {
        val builder = HttpRequest.newBuilder()
            .uri(URI(url))   // TODO catch exceptions?
            .GET()

        if (includeHeaders) {
            headers.forEach { (key, value) ->
                if (key.isNotBlank() && !value.isNullOrBlank()) {
                    builder.header(key, value)
                }
            }
        }

        return builder.build()
    }

    /**
     * Creates a POST [HttpRequest] to the given [url] with the given JSON-encoded [body].
     *
     * @param includeHeaders whether to include [headers] in the returned [HttpRequest]; default true
     */
    protected fun buildPOST(
        url: String,
        body: Any,
        includeHeaders: Boolean = true
    ): HttpRequest {
        val jsonBody = gson.toJson(body)

        val builder = HttpRequest.newBuilder()
            .uri(URI(url))   // TODO catch exceptions?
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))

        if (includeHeaders) {
            headers.forEach { (key, value) -> builder.header(key, value) }
        }

        return builder.build()
    }

    /**
     * Sends this [HttpRequest] and parses the response body as JSON into an object of type [T], returning a pair of the
     * response HTTP status code and the parsed body, or null if the request fails for any reason.
     *
     * TODO there's probably a better way to return the status code
     */
    protected inline fun <reified T> HttpRequest.jsonResponseOrNull(): Pair<Int, T>? {
        return runCatching { httpClient.send(this, HttpResponse.BodyHandlers.ofString()) }
            .getOrNull()
            ?.let {
                try {
                    Pair(it.statusCode(), gson.fromJson(it.body(), T::class.java))
                } catch (ex: JsonSyntaxException) {
                    null
                }
            }
    }
}
