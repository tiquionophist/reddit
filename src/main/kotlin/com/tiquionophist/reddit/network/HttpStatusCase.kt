package com.tiquionophist.reddit.network

/**
 * An abstraction around HTTP status codes which parses them into common cases.
 */
enum class HttpStatusCase {

    SUCCESS, REDIRECT, NOT_FOUND, OTHER;

    companion object {

        @Suppress("MagicNumber")
        fun of(code: Int): HttpStatusCase {
            return when (code) {
                in 200 until 300 -> SUCCESS
                in 300 until 400 -> REDIRECT
                404, 410 -> NOT_FOUND
                else -> OTHER
            }
        }
    }
}
