package com.tiquionophist.reddit

import net.dean.jraw.models.Listing
import net.dean.jraw.models.Submission
import net.dean.jraw.models.TimePeriod
import net.dean.jraw.models.UserHistorySort
import net.dean.jraw.pagination.Paginator
import java.time.Duration

// TODO post karma thresholds (tricky because it might filter out very new posts)
// TODO option to filter posts by SFW/NSFW
// TODO print how many bytes were downloaded (and set a limit)
// TODO option to deduplicate identical url's posted multiple times
// TODO add media providers: v.redd.it, pornhub, youtube, eroshare, erome, twitter, vidble, tumblr, more?
// TODO go through comments for follow-up images, sources, etc
// TODO add CI for tests, detekt, etc

fun main() {
    val start = System.nanoTime()

    Config.load()

    val results = linkedMapOf<Submission, SubmissionSaver.Result>()

    fun Iterable<Listing<*>>.save(type: SubmissionType) {
        forEachIndexed { listingIndex, listing ->
            val submissions = listing.children
                .filterIsInstance(Submission::class.java)
                .filter { !it.isStickied && !it.isSelfPost }

            println("  Got listing ${listingIndex + 1} (${submissions.size} submissions of ${listing.size} items)")
            submissions.forEach { submission ->
                val result = SubmissionSaver.saveSubmission(submission = submission, type = type)
                results[submission] = result
                when (result) {
                    is SubmissionSaver.Result.Saved ->
                        println("    Saved ${submission.redditUrl} to ${result.path}")
                    is SubmissionSaver.Result.Failure ->
                        println("    [WARN] Unable to save ${submission.redditUrl} : ${result.message}")
                }
            }
        }
    }

    println("Downloading saved posts")
    reddit.me()
        .history("saved")
        .limit(Paginator.RECOMMENDED_MAX_LIMIT)
        .build()
        .save(SubmissionType.SAVED_POST)

    val followedUsers = reddit.followedUsers()
    followedUsers.forEachIndexed { userIndex, username ->
        println("Downloading posts by $username [${userIndex + 1} / ${followedUsers.size}]")
        reddit.user(username)
            .history("submitted")
            .limit(Paginator.RECOMMENDED_MAX_LIMIT)
            .sorting(UserHistorySort.TOP)
            .timePeriod(TimePeriod.ALL)
            .build()
            .first() // TODO temporary limit (not using take() since it eagerly loads the next listing as well)
            .let { listOf(it) }
            .save(SubmissionType.FOLLOWED_USER)
    }

    val saved = results.filterOfTypes<Submission, SubmissionSaver.Result.Saved>()
    val alreadySaved = results.filterOfTypes<Submission, SubmissionSaver.Result.AlreadySaved>()
    val ignored = results.filterOfTypes<Submission, SubmissionSaver.Result.Ignored>()
    val notFound = results.filterOfTypes<Submission, SubmissionSaver.Result.NotFound>()
    val unmatched = results.filterOfTypes<Submission, SubmissionSaver.Result.UnMatched>()
    val failures = results.filterOfTypes<Submission, SubmissionSaver.Result.Failure>()

    println()
    println("Done in ${Duration.ofNanos(System.nanoTime() - start).format()}")
    println("  ${saved.size + alreadySaved.size} successful: ${saved.size} new; ${alreadySaved.size} already saved")

    println("  ${ignored.size} ignored:")
    ignored.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${notFound.size} not found:")
    notFound.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${unmatched.size} with no media provider:")
    unmatched.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${failures.size} failed:")
    failures.forEach { println("    ${it.key.redditUrl} | ${it.key.url} : ${it.value.message}") }

    System.exit(0)
}
