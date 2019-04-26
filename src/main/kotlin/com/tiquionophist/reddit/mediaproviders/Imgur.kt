package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.*
import okhttp3.HttpUrl

object Imgur : RestApi() {

    override val headers by lazy {
        mapOf("Authorization" to "Client-ID ${Config.getSecret("imgur.id")}")
    }

    // TODO consider other options to handling .gifv links
    // these are actually html pages (or at least some are) that just have an embedded mp4
    private val hashRegex = """[a-zA-Z0-9]+(\.gifv)?""".toRegex()

    private fun isImgurUrl(url: HttpUrl): Boolean {
        return url.topPrivateDomain() == "imgur.com"
    }

    object Image : MediaProvider {

        override fun matches(url: HttpUrl): Boolean {
            if (!isImgurUrl(url)) return false

            return url.pathSegments().satisfies(
                { hashRegex.matches(it.orEmpty()) },
                { it.isNullOrEmpty() }
            )
        }

        override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
            val imageHash = url.pathSegments().last().substringBefore('.')

            val (statusCode, response) = buildGET("https://api.imgur.com/3/image/$imageHash")
                .jsonResponseOrNull<ResponseModel>()
                ?: return MediaProvider.Result.Error(Throwable("TODO"))

            return when (statusCode) {
                200 ->
                    response.data?.let {
                        MediaProvider.Result.Success(
                            media = Media.File(metadata = metadata, urls = it.urls)
                        )
                    } ?: MediaProvider.Result.Error(Throwable("No data returned by Imgur API"))
                404 -> MediaProvider.Result.NotFound
                else -> MediaProvider.Result.Error(Throwable("Unexpected Imgur status code: $statusCode"))
            }
        }

        private data class ResponseModel(val data: ImageModel?)
    }

    object Album : MediaProvider {

        override fun matches(url: HttpUrl): Boolean {
            if (!isImgurUrl(url)) return false

            return url.pathSegments().satisfies(
                { it == "a" },
                { hashRegex.matches(it.orEmpty()) },
                { it.isNullOrEmpty() }
            )
        }

        override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
            val albumHash = url.pathSegments().last()

            val (statusCode, response) = buildGET("https://api.imgur.com/3/album/$albumHash")
                .jsonResponseOrNull<ResponseModel>()
                ?: return MediaProvider.Result.Error(Throwable("TODO"))

            return when (statusCode) {
                200 ->
                    response.data?.images?.let { images ->
                        if (images.isEmpty()) {
                            MediaProvider.Result.NotFound
                        } else {
                            MediaProvider.Result.Success(
                                media = Media.Album(
                                    metadata = metadata,
                                    children = images.mapIndexed { index, image ->
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
                    } ?: MediaProvider.Result.Error(Throwable("No data returned by Imgur API"))
                404 -> MediaProvider.Result.NotFound
                else -> MediaProvider.Result.Error(Throwable("Unexpected Imgur status code: $statusCode"))
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
        val gifv: String?,  // TODO remove this? (see gifv note above)
        val link: String?
    ) {

        val urls: List<HttpUrl>
            get() = listOfNotNull(mp4, gifv, link)
                .filter { it.isNotBlank() }
                .mapNotNull { HttpUrl.parse(it) }
                .also { println(this) }
    }
}
