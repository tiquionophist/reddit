package com.tiquionophist.reddit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.Calendar

internal class LocalLocationResolverTest {

    private object Fixtures {

        val root: Path = Path.of("root", "path")

        fun resolve(first: String, vararg more: String): Path {
            return root.resolve(Path.of(first, *more))
        }
    }

    // TODO tests for non-windows (with a different filename regex)
    private val localLocationResolver = LocalLocationResolver(root = Fixtures.root, isWindows = true)

    @Test
    fun testGeneric() {
        val metadata = Media.Metadata(
            id = "id",
            author = "author",
            date = Calendar.Builder().setDate(1970, 0, 1).build().time,
            title = "title",
            subreddit = "subreddit"
        )

        testInternal(
            metadata = metadata,
            filenameNoUser = "1970-01-01 - id - title",
            filenameWithUser = "1970-01-01 - author - id - title"
        )
    }

    @Test
    fun testMinimal() {
        val metadata = Media.Metadata(
            id = "id",
            author = "author",
            date = null,
            title = null
        )

        testInternal(
            metadata = metadata,
            filenameNoUser = "id",
            filenameWithUser = "author - id"
        )
    }

    @Test
    fun testComplicated() {
        val metadata = Media.Metadata(
            id = "S4wIrMiDuR",
            author = "OznMlzbvff",
            date = Calendar.Builder().setDate(1982, 7, 8).build().time,
            title = "FOeDFzEnmLQ9vDF",
            subreddit = "bCkEMNFbqi"
        )

        testInternal(
            metadata = metadata,
            filenameNoUser = "1982-08-08 - S4wIrMiDuR - FOeDFzEnmLQ9vDF",
            filenameWithUser = "1982-08-08 - OznMlzbvff - S4wIrMiDuR - FOeDFzEnmLQ9vDF"
        )
    }

    @Test
    fun testEdgeCases() {
        val metadata = Media.Metadata(
            id = "S4wIrMiDuR",
            author = " a b c d 123 ",
            date = Calendar.Builder().setDate(2050, 11, 31).build().time,
            title = "abc  123\t\n~!@#$%^&*()_+{}[];:'\",.<>/?-=~\\|  ...  ",
            subreddit = "bCkEMNFbqi"
        )

        val fixedTitle = "abc 123 ~!@#$%^&_()_+{}[];_'_,.____-=~__"

        testInternal(
            metadata = metadata,
            filenameNoUser = "2050-12-31 - S4wIrMiDuR - $fixedTitle",
            filenameWithUser = "2050-12-31 - a b c d 123 - S4wIrMiDuR - $fixedTitle"
        )
    }

    @Test
    fun testWithPosition() {
        val metadata = Media.Metadata(
            id = "id",
            author = "author",
            date = Calendar.Builder().setDate(1970, 0, 1).build().time,
            title = "title",
            subreddit = "subreddit",
            position = 13
        )

        testInternal(
            metadata = metadata,
            filenameNoUser = "13 - 1970-01-01 - id - title",
            filenameWithUser = "13 - 1970-01-01 - author - id - title"
        )
    }

    private fun testInternal(metadata: Media.Metadata, filenameNoUser: String, filenameWithUser: String) {
        val author = metadata.author.trim()
        val subreddit = metadata.subreddit?.trim()

        val followedUser = localLocationResolver.resolveSubmission(
            metadata = metadata,
            source = MediaSource.FOLLOWED_USER
        )

        assertEquals(Fixtures.resolve("users", author, "all", filenameNoUser), followedUser.primary)
        assertEquals(
            listOfNotNull(
                subreddit?.let { Fixtures.resolve("users", author, it, filenameNoUser) },
                subreddit?.let { Fixtures.resolve("subreddits", it, filenameWithUser) },
                Fixtures.resolve("all", filenameWithUser)
            ).toSet(),
            followedUser.secondaries.toSet()
        )

        val savedPost = localLocationResolver.resolveSubmission(
            metadata = metadata,
            source = MediaSource.SAVED_POST
        )

        assertEquals(Fixtures.resolve("saved", "all", filenameNoUser), savedPost.primary)
        assertEquals(
            listOfNotNull(
                subreddit?.let { Fixtures.resolve("saved", it, filenameNoUser) },
                subreddit?.let { Fixtures.resolve("subreddits", it, filenameWithUser) },
                Fixtures.resolve("all", filenameWithUser)
            ).toSet(),
            savedPost.secondaries.toSet()
        )
    }
}
