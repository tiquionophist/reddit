package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Config
import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import com.tiquionophist.reddit.network.RestApi
import com.tiquionophist.reddit.satisfies
import okhttp3.HttpUrl

object Imgur : RestApi() {
    override val headers by lazy {
        mapOf("Authorization" to "Client-ID ${Config.getSecret("imgur.id")}")
    }

    // TODO consider other options to handling .gifv links
    // these are actually html pages (or at least some are) that just have an embedded mp4
    private val hashRegex = """[a-zA-Z0-9]+(\.gifv)?""".toRegex()

    private fun HttpUrl.isImgurUrl() = topPrivateDomain() == "imgur.com"

    object Image : MediaProvider {
        override fun matches(url: HttpUrl): Boolean {
            if (!url.isImgurUrl()) return false

            return url.pathSegments().satisfies(
                { hashRegex.matches(it.orEmpty()) },
                { it.isNullOrEmpty() }
            )
        }

        override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
            val imageHash = url.pathSegments().last().substringBefore('.')

            val response = buildGET("https://api.imgur.com/3/image/$imageHash").jsonResponse<ResponseModel>()

            return when (response) {
                is JsonResponse.Success ->
                    response.body.data?.urls?.takeIf { it.isNotEmpty() }?.let {
                        MediaProvider.Result.Success(media = Media.File(metadata = metadata, urls = it))
                    } ?: MediaProvider.Result.Error("No data returned by Imgur API")
                is JsonResponse.NotFound -> MediaProvider.Result.NotFound
                is JsonResponse.Error -> MediaProvider.Result.Error("Imgur API error", response.cause)
            }
        }

        @Suppress("UnusedPrivateClass") // detekt false positive
        private data class ResponseModel(val data: ImageModel?)
    }

    object Album : MediaProvider {
        override fun matches(url: HttpUrl): Boolean {
            if (!url.isImgurUrl()) return false

            return url.pathSegments().satisfies(
                { it == "a" || it == "gallery" },
                { hashRegex.matches(it.orEmpty()) },
                { it.isNullOrEmpty() }
            )
        }

        override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
            val albumHash = url.pathSegments().last()

            val response = buildGET("https://api.imgur.com/3/album/$albumHash/images").jsonResponse<ResponseModel>()

            return when (response) {
                is JsonResponse.Success ->
                    response.body.data?.let { result(metadata, it) }
                        ?: MediaProvider.Result.Error("No data returned by Imgur API")
                is JsonResponse.NotFound -> MediaProvider.Result.NotFound
                is JsonResponse.Error -> MediaProvider.Result.Error("Imgur API error", response.cause)
            }
        }

        private fun result(metadata: Media.Metadata, images: List<ImageModel>): MediaProvider.Result {
            val files = images
                .mapIndexed { index, image ->
                    val urls = image.urls
                    when {
                        image.id.isNullOrBlank() -> null
                        urls.isEmpty() -> null
                        else -> Media.File(
                            metadata = metadata.copy(
                                id = image.id,
                                date = null,
                                title = image.title?.takeIf { it.isNotBlank() } ?: image.description,
                                position = index + 1
                            ),
                            urls = urls
                        )
                    }
                }
                .filterNotNull()

            return if (files.isEmpty()) {
                MediaProvider.Result.NotFound
            } else {
                MediaProvider.Result.Success(media = Media.Album(metadata = metadata, children = files))
            }
        }

        @Suppress("UnusedPrivateClass") // detekt false positive
        private data class ResponseModel(val data: List<ImageModel>?)
    }

    private data class ImageModel(
        val id: String?,
        val title: String?,
        val description: String?,
        val mp4: String?,
        val link: String?
    ) {
        val urls: List<HttpUrl>
            get() = listOfNotNull(mp4, link)
                .filter { it.isNotBlank() }
                .mapNotNull { HttpUrl.parse(it) }
    }
}
