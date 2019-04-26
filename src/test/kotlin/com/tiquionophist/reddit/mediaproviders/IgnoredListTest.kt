package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class IgnoredListTest {

    @Test
    fun testMatches() {
        IgnoredList.assertMatches("https://sugarcookie.com/some/path")
        IgnoredList.assertMatches("https://sugarcookie.xxx/some/path")
        IgnoredList.assertMatches("https://harrietsugarcookie.com/some/path")
        IgnoredList.assertMatches("https://subdomain.sugarcookie.com/some/path")

        IgnoredList.assertMatches("https://reddit.com/r/all")
        IgnoredList.assertMatches("http://reddit.com/r/all")
        IgnoredList.assertMatches("https://www.reddit.com/r/all")
        IgnoredList.assertMatches("https://reddit.com/r/some_subRedditName")

        IgnoredList.assertDoesNotMatch("https://imgur.com/abc.png")
        IgnoredList.assertDoesNotMatch("https://imgur.com/abc")
        IgnoredList.assertDoesNotMatch("https://imgur.com/a/abc")
        IgnoredList.assertDoesNotMatch("https://gfycat.com/abc")
        IgnoredList.assertDoesNotMatch("https://reddit.com/abc")
        IgnoredList.assertDoesNotMatch("https://reddit.com/")
        IgnoredList.assertDoesNotMatch("https://reddit.com/u/abc")
    }
}
