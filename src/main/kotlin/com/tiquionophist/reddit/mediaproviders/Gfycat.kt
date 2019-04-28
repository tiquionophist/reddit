package com.tiquionophist.reddit.mediaproviders

import com.google.gson.annotations.SerializedName
import com.tiquionophist.reddit.Config
import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import com.tiquionophist.reddit.network.RestApi
import com.tiquionophist.reddit.satisfies
import okhttp3.HttpUrl

object Gfycat : RestApi(), MediaProvider {

    private val hashRegex = """[a-zA-Z\-]+""".toRegex()

    override val headers
        get() = mapOf("Authorization" to accessToken)

    private fun HttpUrl.isGfycatUrl() = topPrivateDomain() == "gfycat.com"

    // TODO check if the accessToken is expired (or will soon) and get a new one if so
    private var accessToken: String? = null
        get() {
            field?.let { return it }

            val response = buildPOST(
                url = "https://api.gfycat.com/v1/oauth/token",
                body = mapOf(
                    "grant_type" to "client_credentials",
                    "client_id" to Config.getSecret("gfycat.id"),
                    "client_secret" to Config.getSecret("gfycat.secret")
                ),
                includeHeaders = false
            ).jsonResponse<AccessTokenResponseModel>()

            // TODO check if the returned status code is a 401 indicating invalid credentials
            return (response as? JsonResponse.Success)
                ?.body
                ?.accessToken
                ?.takeIf { it.isNotBlank() }
                ?.also { field = it }
        }

    override fun matches(url: HttpUrl): Boolean {
        if (!url.isGfycatUrl()) return false

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

        val response = buildGET(url = "https://api.gfycat.com/v1/gfycats/$gfyId").jsonResponse<LookupResponseModel>()

        return when (response) {
            is JsonResponse.Success ->
                when (response.statusCode) {
                    200 ->
                        response.body.gfyItem?.let {
                            // TODO return NotFound (or Error?) if it.urls is empty
                            MediaProvider.Result.Success(media = Media.File(metadata = metadata, urls = it.urls))
                        } ?: MediaProvider.Result.Error("No gfyItem returned by Gfycat API")
                    404 -> MediaProvider.Result.NotFound
                    else -> MediaProvider.Result.Error("Unexpected Gfycat API status code: ${response.statusCode}")
                }
            is JsonResponse.Error -> MediaProvider.Result.Error("Gfycat API error", response.cause)
        }
    }

    private data class AccessTokenResponseModel(@SerializedName("access_token") val accessToken: String?)

    private data class LookupResponseModel(val gfyItem: GfyModel?)

    private data class GfyModel(
        val webmUrl: String?,
        val mp4Url: String?,
        val gifUrl: String?,
        val mobileUrl: String?,
        val miniUrl: String?
    ) {

        val urls: List<HttpUrl>
            get() = listOfNotNull(webmUrl, mp4Url, gifUrl, mobileUrl, miniUrl)
                .filter { url -> url.isNotBlank() }
                .mapNotNull { url -> HttpUrl.parse(url) }
    }
}
