package com.tiquionophist.reddit

import java.nio.file.Path
import java.text.SimpleDateFormat

/**
 * Represents the location(s) for a particular [Media]; it should always be downloaded to a single [primary] location
 * and multiple optional [secondaries] which should be (hard) links to the [primary].
 */
data class LocalLocation(
    val primary: Path,
    val secondaries: List<Path>
)

/**
 * Determines the [LocalLocation] to which [Media] should be saved; this object should be the only place that filenames
 * and directories are created or modified in order to simplify consistency (i.e. to make sure the filenames don't
 * change over time).
 */
class LocalLocationResolver(
    root: Path = Path.of(System.getProperty("user.home"), "Pictures", "Reddit Downloads"),
    isWindows: Boolean = System.getProperty("os.name").startsWith("Windows")
) {
    private val usersDir = root.resolve("users")
    private val subredditsDir = root.resolve("subreddits")
    private val savedDir = root.resolve("saved")
    private val allDir = root.resolve("all")

    private val invalidFilenameRegex = if (isWindows) {
        """[\\/:*?"<>|]""".toRegex()
    } else {
        """[^\w \-]""".toRegex()
    }

    private fun String.normalizeFilename(): String {
        return this
            .replace(invalidFilenameRegex, "_")
            .replace(whitespaceRegex, " ")
            .trimStart()
            .trimEnd { it == '.' || it.isWhitespace() } // directories with trailing periods are messed up
            .take(filenameMaxLength)
    }

    private fun Media.Metadata.filename(author: String? = null): String {
        return listOfNotNull(
            position?.toString(),
            date?.let { dateFormat.format(it) },
            author,
            id,
            title?.takeIf { it.isNotBlank() }
        )
            .joinToString(separator = " - ")
            .normalizeFilename()
    }

    /**
     * Determines the [LocalLocation] for the given [metadata] with the given [source].
     */
    fun resolveSubmission(metadata: Media.Metadata, source: MediaSource): LocalLocation {
        val primaryDir = when (source) {
            MediaSource.FOLLOWED_USER -> usersDir.resolve(metadata.author.normalizeFilename())
            MediaSource.SAVED_POST -> savedDir
        }

        val filenameNoUser = metadata.filename()
        val filenameWithUser = metadata.filename(author = metadata.author)

        return LocalLocation(
            primary = primaryDir.resolve("all").resolve(filenameNoUser),
            secondaries = listOfNotNull(
                metadata.subreddit?.let { primaryDir.resolve(it.normalizeFilename()).resolve(filenameNoUser) },
                metadata.subreddit?.let { subredditsDir.resolve(it.normalizeFilename()).resolve(filenameWithUser) },
                allDir.resolve(filenameWithUser)
            )
        )
    }

    /**
     * Resolves the given new [metadata] to a new [LocalLocation] relative to the given [base].
     *
     * That is, if the [Media] associated with [metadata] is a child of another [Media] saved at [base] (e.g. files are
     * children of an album); this function determines the [LocalLocation] for the child.
     */
    fun resolveRelative(metadata: Media.Metadata, base: LocalLocation): LocalLocation {
        val filename = metadata.filename()

        return LocalLocation(
            primary = base.primary.resolve(filename),
            secondaries = base.secondaries.map { it.resolve(filename) }
        )
    }

    companion object {
        private val whitespaceRegex = """\s+""".toRegex()

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

        // TODO this might be filesystem/os-dependent
        private const val filenameMaxLength = 250
    }
}
