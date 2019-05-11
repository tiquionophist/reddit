package com.tiquionophist.reddit

import okhttp3.HttpUrl
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class UrlPermuter(
    private val host: String,
    subdomains: List<String> = emptyList(),
    allowHttp: Boolean = true,
    allowWww: Boolean = true,
    allowTrailingSlash: Boolean = true
) {
    private val protocols = if (allowHttp) listOf("https://", "http://") else listOf("https://")
    private val subdomains = subdomains.plus(if (allowWww) listOf("", "www.") else listOf(""))
    private val suffixes = if (allowTrailingSlash) listOf("", "/") else listOf("")

    fun permute(paths: List<String>): List<String> {
        return paths.flatMap { path ->
            protocols.flatMap { protocol ->
                subdomains.flatMap { subdomain ->
                    suffixes.map { suffix ->
                        "$protocol$subdomain$host/$path$suffix"
                    }
                }
            }
        }
    }
}

fun MediaProvider.assertMatches(url: String) {
    assertTrue(matches(HttpUrl.parse(url)!!), "Should have matched url but did not: $url")
}

fun MediaProvider.assertDoesNotMatch(url: String) {
    assertFalse(matches(HttpUrl.parse(url)!!), "Should not have matched url but did: $url")
}
