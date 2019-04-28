package com.tiquionophist.reddit

import com.tiquionophist.reddit.network.DownloadBodyHandler
import net.dean.jraw.models.Submission
import okhttp3.HttpUrl
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.file.Path
import java.text.SimpleDateFormat

class SubmissionSaver(private val root: Path) {

    companion object {

        private val isWindows = System.getProperty("os.name").startsWith("Windows")
        private val invalidFilenameRegex = if (isWindows) {
            """[\\/:*?"<>|]""".toRegex()
        } else {
            """[^\w \-]""".toRegex()
        }

        private val whitespaceRegex = """\s+""".toRegex()

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

        // TODO this might be filesystem/os-dependent
        private const val filenameMaxLength = 250

        private fun String.normalizeFilename(): String {
            return this
                .replace(invalidFilenameRegex, "_")
                .replace(whitespaceRegex, " ")
                .trimEnd { it == '.' || it.isWhitespace() } // directories with trailing periods are messed up
                .take(filenameMaxLength)
        }

        private val Media.Metadata.localFilename: String
            get() {
                return listOfNotNull(
                    position?.toString(),
                    date?.let { dateFormat.format(it) },
                    id,
                    title?.takeIf { it.isNotBlank() }
                )
                    .joinToString(separator = " - ")
                    .normalizeFilename()
            }

        private fun Media.Metadata.existsIn(path: Path): Boolean {
            val filename = localFilename
            // TODO maybe try to cache the list of filenames rather than doing a new query each time
            return path.toFile().list().any { it.startsWith(filename) }
        }
    }

    sealed class Result {
        data class Saved(val path: Path) : Result()
        object AlreadySaved : Result()
        object Ignored : Result()
        object NotFound : Result()
        object UnMatched : Result()
        data class Failure(val message: String, val cause: Throwable = Throwable(message)) : Result()
    }

    private val httpClient = HttpClient.newHttpClient()

    fun saveUserPost(submission: Submission): Result {
        return save(
            submission = submission,
            base = root.resolve(submission.author)
        )
    }

    private fun save(submission: Submission, base: Path): Result {
        val url = HttpUrl.parse(submission.url)
            ?: return Result.Failure("Malformed submission URL: ${submission.url}")

        val mediaProvider = mediaProviders.firstOrNull { it.matches(url) } ?: return Result.UnMatched

        // TODO make this a Submission extension method?
        val metadata = Media.Metadata(
            id = submission.id,
            date = submission.created,
            title = submission.title
        )

        // TODO avoid doing this for every submission
        val baseFile = base.toFile()
        if (!baseFile.isDirectory && !baseFile.mkdirs()) {
            return Result.Failure("Unable to create directory: $base")
        }

        if (metadata.existsIn(base)) {
            return Result.AlreadySaved
        }

        return when (val mediaResult = mediaProvider.resolveMedia(metadata = metadata, url = url)) {
            is MediaProvider.Result.Success ->
                runCatching { saveMedia(media = mediaResult.media, base = base) }
                    .getOrElse { Result.Failure(message = "Failed to save media", cause = it) }
            is MediaProvider.Result.Error -> Result.Failure(
                message = "Error resolving media for ${submission.url}: ${mediaResult.message}",
                cause = mediaResult.cause
            )
            is MediaProvider.Result.NotFound -> Result.NotFound
            is MediaProvider.Result.Ignored -> Result.Ignored
        }
    }

    private fun saveMedia(media: Media, base: Path): Result {
        return when (media) {
            is Media.File -> saveFile(file = media, base = base)
            is Media.Album -> saveAlbum(album = media, base = base)
        }
    }

    private fun saveFile(file: Media.File, base: Path): Result {
        if (file.urls.isEmpty()) {
            return Result.Failure(message = "File has no urls")
        }

        val filename = base.resolve(file.metadata.localFilename)

        val results: Map<HttpUrl, Result> = file.urls.associateWith { url ->
            val request = HttpRequest.newBuilder()
                .uri(url.uri())
                .GET()
                .build()

            // TODO consider sending async
            when (val result = httpClient.send(request, DownloadBodyHandler(filename)).body()) {
                is DownloadBodyHandler.Result.Success -> return Result.Saved(path = result.path)
                is DownloadBodyHandler.Result.NotFound -> Result.NotFound
                is DownloadBodyHandler.Result.UnknownContentType ->
                    Result.Failure("Unknown content-type: ${result.contentType}")
                is DownloadBodyHandler.Result.UnexpectedResponse ->
                    Result.Failure("Unexpected HTTP response status code: ${result.statusCode}")
            }
        }

        return Result.Failure(message = "Unable to fetch any of the media urls: $results")
    }

    private fun saveAlbum(album: Media.Album, base: Path): Result {
        val directory = base.resolve(album.metadata.localFilename)
        val directoryFile = directory.toFile()

        if (!directoryFile.mkdirs()) {
            return Result.Failure("Unable to create directory: $directory")
        }

        for (child in album.children) {
            val result = saveMedia(media = child, base = directory)
            if (result is Result.Failure) {
                // TODO warn or pass back failures somehow - disk state corrupted if this fails!
                directoryFile.delete()

                // TODO this only really works for nesting one-deep (which is fine for the moment)
                return Result.Failure(message = "Failed album child", cause = result.cause)
            }
        }

        return Result.Saved(path = directory)
    }
}
