package com.tiquionophist.reddit.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HttpStatusCaseTest {

    @Test
    fun testCodes() {
        testCase(HttpStatusCase.SUCCESS, 200, 201, 202)
        testCase(HttpStatusCase.REDIRECT, 300, 301, 302, 307, 308)
        testCase(HttpStatusCase.NOT_FOUND, 403, 404, 410)
        testCase(HttpStatusCase.OTHER, 400, 401, 500, 503, 100, 101)
    }

    private fun testCase(case: HttpStatusCase, vararg codes: Int) {
        codes.forEach { assertEquals(case, HttpStatusCase.of(it)) }
    }
}
