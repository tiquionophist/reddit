package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class DirectLinkTest {

    @Test
    fun testMatches() {
        // TODO flesh these out
        DirectLink.assertMatches("https://i.imgur.com/abcd.png")
        DirectLink.assertMatches("https://i.imgur.com/abcd.jpg")
        DirectLink.assertMatches("https://i.imgur.com/abcd.gif")
        DirectLink.assertMatches("https://i.imgur.com/abcd.mp4")
        DirectLink.assertMatches("https://i.imgur.com/abcd.webm")

        DirectLink.assertMatches("http://any.website/abcd.png")
        DirectLink.assertMatches("https://i.imgur.com/abcd.png?1#2")

        DirectLink.assertDoesNotMatch("https://i.imgur.com")
        DirectLink.assertDoesNotMatch("https://i.imgur.com/abcd")
        DirectLink.assertDoesNotMatch("https://i.imgur.com/abcd.txt")
        DirectLink.assertDoesNotMatch("https://i.imgur.com/abcd.gifv")
        DirectLink.assertDoesNotMatch("https://i.imgur.com/abcd.png.nothing")
    }
}
