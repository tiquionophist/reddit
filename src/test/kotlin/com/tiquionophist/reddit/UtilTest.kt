package com.tiquionophist.reddit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.Duration

internal class UtilTest {

    @Test
    fun testListSatisfies() {
        assert(
            listOf(1, 2).satisfies(
                { it == 1 },
                { it == 2 }
            )
        )

        assert(
            listOf(1, 2).satisfies(
                { it == 1 },
                { it == 2 },
                { it == null }
            )
        )

        assertFalse(
            listOf(1, 2).satisfies(
                { it == 1 }
            )
        )

        assertFalse(
            listOf(1, 2).satisfies(
                { it == 2 },
                { it == 1 }
            )
        )
    }

    @Test
    fun testDurationFormat() {
        assertEquals("<1sec", Duration.ZERO.format())
        assertEquals("<1sec", Duration.ofNanos(100).format())
        assertEquals("3sec", Duration.ofSeconds(3).format())
        assertEquals("1min", Duration.ofSeconds(60).format())
        assertEquals("1min 40sec", Duration.ofSeconds(100).format())
        assertEquals("2days 3hr 4min 5sec", Duration.ofDays(2).plusHours(3).plusMinutes(4).plusSeconds(5).format())
    }

    @Test
    fun testFormatByteSize() {
        assertEquals("42 B", formatByteSize(42))
        assertEquals("1.0 KB", formatByteSize(1024))
        assertEquals("1.0 KB", formatByteSize(1025))
        assertEquals("1.7 KB", formatByteSize(1728))
        assertEquals("1.0 MB", formatByteSize(1024 * 1024))
        assertEquals("3.1 MB", formatByteSize((1024 * 1024 * 3) + (1024 * 100)))
        assertEquals("8.0 EB", formatByteSize(Long.MAX_VALUE))
    }
}
