package com.tiquionophist.reddit

import java.io.IOException
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow

// TODO document
class DownloadBodyHandler(private val path: Path) : HttpResponse.BodyHandler<DownloadBodyHandler.Result> {

    sealed class Result {
        class Success(val path: Path) : Result()
        object NotFound : Result()
        class UnknownContentType(val contentType: String?) : Result()
        class UnexpectedResponse(val statusCode: Int) : Result()
    }

    companion object {

        private val contentTypes = mapOf(
            "image/jpeg" to ".jpg",
            "image/png" to ".png",
            "image/gif" to ".gif",
            "video/mp4" to ".mp4",
            "video/webm" to ".webm"
        )

        private fun Path.withExtension(extension: String): Path {
            return resolveSibling(fileName.toString() + extension)
        }
    }

    override fun apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<Result> {
        // TODO flesh out status codes
        return when (responseInfo.statusCode()) {
            200 -> {
                val contentType = responseInfo.headers().firstValue("content-type").orElse(null)
                contentType?.let {
                    contentTypes[contentType]?.let { extension ->
                        DownloadSubscriber(path.withExtension(extension))
                    } ?: CompletedSubscriber(Result.UnknownContentType(contentType))
                } ?: CompletedSubscriber(Result.UnknownContentType(null))
            }
            302, 404 -> CompletedSubscriber(Result.NotFound)
            else -> CompletedSubscriber(Result.UnexpectedResponse(responseInfo.statusCode()))
        }
    }

    private class DownloadSubscriber(private val path: Path) : HttpResponse.BodySubscriber<Result> {

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
            future.complete(Result.Success(path))
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
