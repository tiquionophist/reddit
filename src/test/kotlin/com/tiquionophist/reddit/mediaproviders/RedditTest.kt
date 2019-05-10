package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class RedditTest {

    @Test
    fun testMatches() {
        Reddit.assertMatches("http://i.reddituploads.com")
        Reddit.assertMatches("http://i.reddituploads.com/any1hing?1#2")

        Reddit.assertDoesNotMatch("https://reddit.com/")
        Reddit.assertDoesNotMatch("https://external-preview.redd.it/abc")
        Reddit.assertDoesNotMatch("https://reddit.com/abc")
        Reddit.assertDoesNotMatch("https://reddit.com/r/abc")
        Reddit.assertDoesNotMatch("https://reddit.com/u/abc")
        Reddit.assertDoesNotMatch("http://v.redd.it/abc")
    }
}
