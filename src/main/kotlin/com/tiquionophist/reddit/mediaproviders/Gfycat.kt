package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.*
import okhttp3.HttpUrl

object Gfycat : RestApi(), MediaProvider {

    private val hashRegex = """[a-zA-Z]+""".toRegex()

    override val headers
        get() = mapOf("Authorization" to accessToken)

    // TODO check if the accessToken is expired (or will soon) and get a new one if so
    private var accessToken: String? = null
        get() {
            field?.let { return it }

            // TODO check if the returned status code is a 401 indicating invalid credentials
            val response = buildPOST(
                url = "https://api.gfycat.com/v1/oauth/token",
                body = mapOf(
                    "grant_type" to "client_credentials",
                    "client_id" to Config.getSecret("gfycat.id"),
                    "client_secret" to Config.getSecret("gfycat.secret")
                ),
                includeHeaders = false
            )
                .jsonResponseOrNull<AccessTokenResponseModel>()
                ?.second

            return response?.access_token?.takeIf { it.isNotBlank() }.also { field = it }
        }

    override fun matches(url: HttpUrl): Boolean {
        if (url.topPrivateDomain() != "gfycat.com") return false

        // TODO this could be more elegant
        val path = url.pathSegments()
        return path.satisfies(
            { hashRegex.matches(it.orEmpty()) },
            { it.isNullOrEmpty() }
        ) || path.satisfies(
            { it == "gifs" },
            { it == "detail" },
            { hashRegex.matches(it.orEmpty()) },
            { it.isNullOrEmpty() }
        )
    }

    override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
        val gfyId = url.pathSegments().last()

        val (statusCode, response) = buildGET(url = "https://api.gfycat.com/v1/gfycats/$gfyId")
            .jsonResponseOrNull<LookupResponseModel>() ?: return MediaProvider.Result.Error(NullPointerException())

        return when (statusCode) {
            200 ->
                response.gfyItem?.let {
                    val urls = listOfNotNull(it.webmUrl, it.mp4Url, it.gifUrl, it.mobileUrl, it.miniUrl)
                        .filter { url -> url.isNotBlank() }
                        .mapNotNull { url -> HttpUrl.parse(url) }

                    MediaProvider.Result.Success(
                        media = Media.File(
                            metadata = metadata,
                            urls = urls
                        )
                    )
                } ?: MediaProvider.Result.Error(Throwable("No gfyItem returned by Gfycat API"))
            404 -> MediaProvider.Result.NotFound
            else -> MediaProvider.Result.Error(Throwable("Unexpected Gfycat status code: $statusCode"))
        }
    }

    private data class AccessTokenResponseModel(val access_token: String?)

    private data class LookupResponseModel(val gfyItem: GfyModel?)

    private data class GfyModel(
        val webmUrl: String?,
        val mp4Url: String?,
        val gifUrl: String?,
        val mobileUrl: String?,
        val miniUrl: String?
    )
}
