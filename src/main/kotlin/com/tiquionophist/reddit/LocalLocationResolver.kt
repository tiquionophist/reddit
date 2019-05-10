package com.tiquionophist.reddit

import net.dean.jraw.models.Submission
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
object LocalLocationResolver {

    // TODO move to a config file or similar
    private val root = Path.of(System.getProperty("user.home"), "Pictures", "Reddit Downloads")
    private val usersDir = root.resolve("users")
    private val subredditsDir = root.resolve("subreddits")
    private val allDir = root.resolve("all")

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
            .trimStart()
            .trimEnd { it == '.' || it.isWhitespace() } // directories with trailing periods are messed up
            .take(filenameMaxLength)
    }

    private fun Media.Metadata.filename(user: String? = null): String {
        return listOfNotNull(
            position?.toString(),
            date?.let { dateFormat.format(it) },
            user,
            id,
            title?.takeIf { it.isNotBlank() }
        )
            .joinToString(separator = " - ")
            .normalizeFilename()
    }

    /**
     * Determines the [LocalLocation] to which the [submission] with the given [metadata] should be saved.
     */
    fun resolve(submission: Submission, metadata: Media.Metadata): LocalLocation {
        val user = submission.author
        val subreddit = submission.subreddit

        val userDir = usersDir.resolve(user)
        val subredditDir = subredditsDir.resolve(subreddit)

        val filenameNoUser = metadata.filename()
        val filenameWithUser = metadata.filename(user = user)

        return LocalLocation(
            primary = userDir.resolve("all").resolve(filenameNoUser),
            secondaries = listOf(
                userDir.resolve(subreddit).resolve(filenameNoUser),
                subredditDir.resolve(filenameWithUser),
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
}
