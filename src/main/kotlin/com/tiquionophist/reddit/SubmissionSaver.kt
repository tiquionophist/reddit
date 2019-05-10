package com.tiquionophist.reddit

import com.tiquionophist.reddit.network.DownloadBodyHandler
import net.dean.jraw.models.Submission
import okhttp3.HttpUrl
import java.io.IOException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.file.Files
import java.nio.file.Path

object SubmissionSaver {

    sealed class Result {
        data class Saved(val path: Path) : Result()
        object AlreadySaved : Result()
        object Ignored : Result()
        object NotFound : Result()
        object NotMatched : Result()
        data class Failure(val message: String, val cause: Throwable = Throwable(message)) : Result()
    }

    private val httpClient = HttpClient.newHttpClient()

    fun saveSubmission(submission: Submission, type: SubmissionType): Result {
        val url = HttpUrl.parse(submission.url)
            ?: return Result.Failure("Malformed submission URL: ${submission.url}")

        val local = LocalLocationResolver.resolveSubmission(submission = submission, type = type)

        return save(url = url, metadata = submission.metadata, local = local)
    }

    private fun save(url: HttpUrl, metadata: Media.Metadata, local: LocalLocation): Result {
        val mediaProvider = mediaProviders.firstOrNull { it.matches(url) } ?: return Result.NotMatched

        // TODO avoid doing this for every submission
        try {
            Files.createDirectories(local.primary.parent)
        } catch (ex: IOException) {
            return Result.Failure(message = "Unable to create directory: ${local.primary.parent}", cause = ex)
        }

        val filename = local.primary.fileName.toString()

        // since we don't know the file's extension, we check whether any of the files in the directory start with the
        // desired filename; this is very inefficient and potentially finds false-positives
        // TODO maybe try to cache the list of filenames rather than doing a new query each time
        if (Files.list(local.primary.parent).anyMatch { it.fileName.toString().startsWith(filename) }) {
            return Result.AlreadySaved
        }

        return when (val mediaResult = mediaProvider.resolveMedia(metadata = metadata, url = url)) {
            is MediaProvider.Result.Success ->
                runCatching { saveMedia(media = mediaResult.media, local = local) }
                    .getOrElse { Result.Failure(message = "Failed to save media", cause = it) }
            is MediaProvider.Result.Error -> Result.Failure(
                message = "Error resolving media for $url: ${mediaResult.message}",
                cause = mediaResult.cause
            )
            is MediaProvider.Result.NotFound -> Result.NotFound
            is MediaProvider.Result.Ignored -> Result.Ignored
        }
    }

    private fun saveMedia(media: Media, local: LocalLocation): Result {
        return when (media) {
            is Media.File -> saveFile(file = media, local = local)
            is Media.Album ->
                if (media.children.size == 1) {
                    val first = media.children.first()
                    if (first is Media.File) {
                        saveFile(file = first.copy(metadata = media.metadata), local = local)
                    } else {
                        saveAlbum(album = media, local = local)
                    }
                } else {
                    saveAlbum(album = media, local = local)
                }
        }
    }

    private fun saveFile(file: Media.File, local: LocalLocation): Result {
        if (file.urls.isEmpty()) {
            return Result.Failure(message = "File has no urls")
        }

        val results: Map<HttpUrl, Result> = file.urls.associateWith { url ->
            val request = HttpRequest.newBuilder()
                .uri(url.uri())
                .GET()
                .build()

            // TODO consider sending async
            when (val result = httpClient.send(request, DownloadBodyHandler(local.primary)).body()) {
                is DownloadBodyHandler.Result.Success -> {
                    try {
                        linkSecondaries(result, local)
                    } catch (ex: IOException) {
                        return Result.Failure(message = "Unable to create link to ${result.path}", cause = ex)
                    }

                    return Result.Saved(path = result.path)
                }
                is DownloadBodyHandler.Result.NotFound -> Result.NotFound
                is DownloadBodyHandler.Result.Redirect ->
                    // TODO consider limiting the number of redirects
                    HttpUrl.parse(result.location)?.let { redirectUrl ->
                        save(
                            url = redirectUrl,
                            metadata = file.metadata,
                            local = local
                        )
                    } ?: Result.Failure(message = "Malformed redirect URL: ${result.location}")
                is DownloadBodyHandler.Result.UnknownContentType ->
                    Result.Failure(message = "Unknown content-type: ${result.contentType}")
                is DownloadBodyHandler.Result.UnexpectedResponse ->
                    Result.Failure(message = "Unexpected HTTP response status code: ${result.statusCode}")
            }
        }

        return when {
            results.size == 1 -> results.values.first()
            results.all { it.value is Result.NotFound } -> Result.NotFound
            else -> Result.Failure(message = "Unable to fetch any of the media urls: $results")
        }
    }

    private fun saveAlbum(album: Media.Album, local: LocalLocation): Result {
        try {
            Files.createDirectories(local.primary)
        } catch (ex: IOException) {
            return Result.Failure(message = "Unable to create directory: ${local.primary}", cause = ex)
        }

        for (child in album.children) {
            val result = saveMedia(
                media = child,
                local = LocalLocationResolver.resolveRelative(metadata = child.metadata, base = local)
            )
            if (result is Result.Failure) {
                try {
                    recursiveDelete(local.primary)
                    local.secondaries.forEach { recursiveDelete(it) }
                } catch (ex: IOException) {
                    println("[ERROR] Failed to clean up partially saved album, disk state is corrupted: $local")
                }

                // TODO this only really works for nesting one-deep (which is fine for the moment)
                return Result.Failure(message = "Failed album child", cause = result.cause)
            }
        }

        return Result.Saved(path = local.primary)
    }

    private fun linkSecondaries(result: DownloadBodyHandler.Result.Success, local: LocalLocation) {
        local.secondaries.forEach { path ->
            // TODO avoid attempting to create the directory for every submission
            try {
                Files.createDirectories(path.parent)
            } catch (ex: IOException) {
                try {
                    Files.deleteIfExists(local.primary)
                    local.secondaries.forEach { Files.deleteIfExists(it) }
                } catch (ex: IOException) {
                    println("[ERROR] Unable to clean up partially saved file, disk state corrupted for $local")
                }

                throw ex
            }

            val pathWithExtension = path.withExtension(result.extension)

            // the link might already exist if the same post was downloaded from multiple places, e.g. both a followed
            // user and a saved post, or if the user deleted the primary file but not secondaries
            // TODO this is an opportunity to consolidate hard links to save disk space
            if (!Files.exists(pathWithExtension)) {
                try {
                    Files.createLink(pathWithExtension, result.path)
                } catch (ex: IOException) {
                    try {
                        Files.deleteIfExists(local.primary)
                        local.secondaries.forEach { Files.deleteIfExists(it) }
                    } catch (ex: IOException) {
                        println("[ERROR] Unable to clean up partially saved file, disk state corrupted for $local")
                    }

                    throw ex
                }
            }
        }
    }
}
