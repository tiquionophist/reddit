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
                    when (response.statusCode) {
                        200 ->
                            response.body.data?.let {
                                // TODO return NotFound (or Error?) if it.urls is empty
                                MediaProvider.Result.Success(media = Media.File(metadata = metadata, urls = it.urls))
                            } ?: MediaProvider.Result.Error("No data returned by Imgur API")
                        404 -> MediaProvider.Result.NotFound
                        else -> MediaProvider.Result.Error("Unexpected Imgur API status code: ${response.statusCode}")
                    }
                is JsonResponse.Error -> MediaProvider.Result.Error("Imgur API error", response.cause)
            }
        }

        private data class ResponseModel(val data: ImageModel?)
    }

    object Album : MediaProvider {

        override fun matches(url: HttpUrl): Boolean {
            if (!url.isImgurUrl()) return false

            return url.pathSegments().satisfies(
                { it == "a" },
                { hashRegex.matches(it.orEmpty()) },
                { it.isNullOrEmpty() }
            )
        }

        override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
            val albumHash = url.pathSegments().last()

            val response = buildGET("https://api.imgur.com/3/album/$albumHash").jsonResponse<ResponseModel>()

            return when (response) {
                is JsonResponse.Success ->
                    when (response.statusCode) {
                        200 ->
                            response.body.data?.let { data ->
                                if (data.images.isNullOrEmpty()) {
                                    MediaProvider.Result.NotFound
                                } else {
                                    MediaProvider.Result.Success(
                                        media = Media.Album(
                                            metadata = metadata,
                                            children = data.images.mapIndexed { index, image ->
                                                image.id?.takeIf { it.isNotBlank() }?.let { id ->
                                                    Media.File(
                                                        metadata = Media.Metadata(
                                                            id = id,
                                                            date = null,
                                                            title = image.title?.takeIf { it.isNotBlank() }
                                                                ?: image.description,
                                                            position = index + 1
                                                        ),
                                                        urls = image.urls
                                                    )
                                                }
                                            }.filterNotNull()
                                        )
                                    )
                                }
                            } ?: MediaProvider.Result.Error("No data returned by Imgur API")
                        404 -> MediaProvider.Result.NotFound
                        else -> MediaProvider.Result.Error("Unexpected Imgur API status code: ${response.statusCode}")
                    }
                is JsonResponse.Error -> MediaProvider.Result.Error("Imgur API error", response.cause)
            }
        }

        private data class ResponseModel(val data: AlbumModel?)

        private data class AlbumModel(val images: List<ImageModel>?)
    }

    private data class ImageModel(
        val id: String?,
        val title: String?,
        val description: String?,
        val mp4: String?,
        val gifv: String?, // TODO remove this? (see gifv note above)
        val link: String?
    ) {

        val urls: List<HttpUrl>
            get() = listOfNotNull(mp4, gifv, link)
                .filter { it.isNotBlank() }
                .mapNotNull { HttpUrl.parse(it) }
    }
}
