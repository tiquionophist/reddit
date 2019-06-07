package com.tiquionophist.reddit

import net.dean.jraw.models.Listing
import net.dean.jraw.models.Submission
import net.dean.jraw.models.TimePeriod
import net.dean.jraw.models.UserHistorySort
import net.dean.jraw.pagination.Paginator
import java.time.Duration

// TODO option to deduplicate identical url's posted multiple times
// TODO add media providers: pornhub, eroshare, erome, twitter, vidble, tumblr, more?
// TODO go through comments for follow-up images, sources, etc
// TODO add CI for tests, detekt, etc
// TODO better logging (log files, levels, channels)
// TODO https-only option?
// TODO option to set mtime to the time of the post

private val saved = linkedMapOf<Submission, SubmissionSaver.Result.Saved>()
private val alreadySaved = linkedMapOf<Submission, SubmissionSaver.Result.AlreadySaved>()
private val ignored = linkedMapOf<Submission, SubmissionSaver.Result.Ignored>()
private val notFound = linkedMapOf<Submission, SubmissionSaver.Result.NotFound>()
private val notMatched = linkedMapOf<Submission, SubmissionSaver.Result.NotMatched>()
private val failures = linkedMapOf<Submission, SubmissionSaver.Result.Failure>()

fun main() {
    val start = System.nanoTime()

    Config.load()

    if (Config.savedPosts) {
        println("Downloading ${AnsiColor.BLUE.color("saved posts")}")
        reddit.me()
            .history("saved")
            .limit(Paginator.RECOMMENDED_MAX_LIMIT)
            .build()
            .save(MediaSource.SAVED_POST)
    }

    if (Config.followedUsers) {
        val followedUsers = reddit.followedUsers().plus(Config.getAdditionalUsers())
        followedUsers.forEachIndexed { userIndex, username ->
            println("Downloading posts by ${AnsiColor.BLUE.color(username)} [${userIndex + 1} / ${followedUsers.size}]")
            reddit.user(username)
                .history("submitted")
                .limit(Paginator.RECOMMENDED_MAX_LIMIT)
                .sorting(UserHistorySort.TOP)
                .timePeriod(TimePeriod.ALL)
                .build()
                .save(MediaSource.FOLLOWED_USER)
        }
    }

    val bytes = saved.values.map { it.bytes }.sum()

    println()
    println("Done in ${Duration.ofNanos(System.nanoTime() - start).format()}")
    print("  ${saved.size + alreadySaved.size} successful: ")
    print("${saved.size} new (${formatByteSize(bytes)}); ")
    print("${alreadySaved.size} already saved")
    println()

    println("  ${ignored.size} ignored:")
    ignored.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${notFound.size} not found:")
    notFound.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${notMatched.size} with no media provider:")
    notMatched.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${failures.size} failed:")
    failures.forEach { println("    ${it.key.redditUrl} | ${it.key.url} : ${it.value.message}") }

    System.exit(0)
}

private fun Iterable<Listing<*>>.save(source: MediaSource) {
    val scoreThreshold = when (source) {
        MediaSource.SAVED_POST -> Config.scoreThresholdSavedPost
        MediaSource.FOLLOWED_USER -> Config.scoreThresholdFollowedUser
    }

    forEachIndexed { listingIndex, listing ->
        val submissions = listing.children
            .filterIsInstance(Submission::class.java)
            .filter { !it.isStickied && !it.isSelfPost }
            .filter { if (it.isNsfw) Config.saveNsfw else Config.saveSfw }
            .filter { it.score > scoreThreshold }

        println("  Got listing ${listingIndex + 1} (${submissions.size} valid submissions of ${listing.size} items)")

        submissions.forEach { submission ->
            val result = SubmissionSaver.saveSubmission(submission = submission, source = source)
            when (result) {
                is SubmissionSaver.Result.Saved -> {
                    saved[submission] = result
                    println("    ${AnsiColor.GREEN.color("Saved")} ${submission.redditUrl} to ${result.path}")
                }
                is SubmissionSaver.Result.AlreadySaved -> alreadySaved[submission] = result
                is SubmissionSaver.Result.Ignored -> ignored[submission] = result
                is SubmissionSaver.Result.NotFound -> notFound[submission] = result
                is SubmissionSaver.Result.NotMatched -> notMatched[submission] = result
                is SubmissionSaver.Result.Failure -> {
                    failures[submission] = result
                    println(
                        "    ${AnsiColor.RED.color("[WARN]")} Unable to save " +
                                "${submission.redditUrl} : ${result.message}"
                    )
                }
            }
        }
    }
}
