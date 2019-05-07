package com.tiquionophist.reddit.network

import com.google.gson.Gson
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
    protected open val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    sealed class JsonResponse<T> {
        class Error<T>(val cause: Throwable) : JsonResponse<T>()
        class Success<T>(val statusCode: Int, val body: T) : JsonResponse<T>()
    }

    /**
     * Creates a GET [HttpRequest] to the given [url].
     *
     * @param includeHeaders whether to include [headers] in the returned [HttpRequest]; default true
     */
    protected fun buildGET(url: String, includeHeaders: Boolean = true): HttpRequest {
        val builder = HttpRequest.newBuilder()
            .uri(URI(url)) // TODO catch exceptions?
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
    protected fun buildPOST(url: String, body: Any, includeHeaders: Boolean = true): HttpRequest {
        val jsonBody = gson.toJson(body)

        val builder = HttpRequest.newBuilder()
            .uri(URI(url)) // TODO catch exceptions?
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))

        if (includeHeaders) {
            headers.forEach { (key, value) -> builder.header(key, value) }
        }

        return builder.build()
    }

    /**
     * Sends this [HttpRequest] and parses the response body as JSON into an object of type [T], returning it wrapped in
     * a [JsonResponse] object which will be a [JsonResponse.Success] if the request and JSON parsing succeeded, or
     * [JsonResponse.Error] if either failed for any reason.
     */
    protected inline fun <reified T> HttpRequest.jsonResponse(): JsonResponse<T> {
        return runCatching {
            httpClient.send(this, HttpResponse.BodyHandlers.ofString()).let {
                JsonResponse.Success(
                    statusCode = it.statusCode(),
                    body = gson.fromJson(it.body(), T::class.java)
                )
            }
        }
            .getOrElse { JsonResponse.Error(it) }
    }
}
