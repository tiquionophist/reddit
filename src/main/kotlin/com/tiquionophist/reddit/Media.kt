package com.tiquionophist.reddit

import net.dean.jraw.models.Submission
import okhttp3.HttpUrl
import java.util.Date

sealed class Media {
    data class Metadata(
        val id: String,
        val author: String,
        val date: Date?,
        val title: String?,
        val subreddit: String? = null,
        val position: Int? = null,
        val submission: Submission? = null
    )

    abstract val metadata: Metadata

    data class File(
        override val metadata: Metadata,
        val urls: List<HttpUrl>
    ) : Media()

    data class Video(
        override val metadata: Metadata,
        val url: HttpUrl
    ) : Media()

    data class Album(
        override val metadata: Metadata,
        val children: List<Media>
    ) : Media()
}
