package com.tiquionophist.reddit.network

import com.tiquionophist.reddit.withExtension
import java.io.IOException
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow

/**
 * A [HttpResponse.BodyHandler] which saves the response body to a local file, providing more options and flexibility
 * than the standard [jdk.internal.net.http.ResponseBodyHandlers.FileDownloadBodyHandler].
 *
 * The downloaded file is saved to [path]; note that [path] should point to a non-existing file with no extension - this
 * class has a mapping from known Content-Type HTTP headers to the extensions that should be appended to [path].
 */
class DownloadBodyHandler(private val path: Path) : HttpResponse.BodyHandler<DownloadBodyHandler.Result> {

    sealed class Result {
        class Success(val path: Path, val extension: String) : Result()
        object NotFound : Result()
        class Redirect(val location: String) : Result()
        class UnknownContentType(val contentType: String?) : Result()
        class UnexpectedResponse(val statusCode: Int) : Result()
    }

    companion object {

        private val contentTypes = mapOf(
            "image/jpeg" to ".jpg",
            "image/png" to ".png",
            "image/gif" to ".gif",
            "video/mp4" to ".mp4",
            "video/webm" to ".webm",

            // this is not a valid Content-Type but it is used by some i.reddituploads.com resources and appears to work
            // with any extension
            "image/*" to ".jpg"
        )
    }

    override fun apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<Result> {
        val statusCode = responseInfo.statusCode()
        return when (HttpStatusCase.of(statusCode)) {
            HttpStatusCase.SUCCESS -> {
                val contentType = responseInfo.headers().firstValue("content-type").orElse(null)
                contentTypes[contentType]?.let { extension ->
                    DownloadSubscriber(path = path.withExtension(extension), extension = extension)
                } ?: CompletedSubscriber(Result.UnknownContentType(contentType))
            }
            HttpStatusCase.REDIRECT -> {
                val location = responseInfo.headers().firstValue("location").orElse(null)
                CompletedSubscriber(Result.Redirect(location))
            }
            HttpStatusCase.NOT_FOUND -> CompletedSubscriber(Result.NotFound)
            HttpStatusCase.OTHER -> CompletedSubscriber(Result.UnexpectedResponse(statusCode))
        }
    }

    private class DownloadSubscriber(
        private val path: Path,
        private val extension: String
    ) : HttpResponse.BodySubscriber<Result> {

        private lateinit var out: FileChannel
        private lateinit var subscription: Flow.Subscription
        private val future = CompletableFuture<Result>()

        override fun onSubscribe(subscription: Flow.Subscription) {
            this.subscription = subscription
            try {
                out = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
            } catch (ex: IOException) {
                future.completeExceptionally(ex)
                subscription.cancel()
                return
            }

            subscription.request(1)
        }

        override fun onNext(item: List<ByteBuffer>) {
            try {
                out.write(item.toTypedArray())
            } catch (ex: IOException) {
                runCatching { out.close() }
                subscription.cancel()
                future.completeExceptionally(ex)
            }
            subscription.request(1)
        }

        override fun onError(throwable: Throwable) {
            future.completeExceptionally(throwable)
        }

        override fun onComplete() {
            runCatching { out.close() }
            future.complete(Result.Success(path = path, extension = extension))
        }

        override fun getBody() = future
    }

    private class CompletedSubscriber(result: Result) : HttpResponse.BodySubscriber<Result> {

        private val future: CompletableFuture<Result> = CompletableFuture.completedFuture(result)

        override fun onSubscribe(subscription: Flow.Subscription) = Unit
        override fun onNext(item: List<ByteBuffer>) = Unit
        override fun onError(throwable: Throwable) = Unit
        override fun onComplete() = Unit
        override fun getBody() = future
    }
}
