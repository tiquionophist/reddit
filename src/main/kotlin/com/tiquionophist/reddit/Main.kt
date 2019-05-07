package com.tiquionophist.reddit

import net.dean.jraw.models.Submission
import net.dean.jraw.models.TimePeriod
import net.dean.jraw.models.UserHistorySort
import net.dean.jraw.pagination.Paginator
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/*
- download all media posted by followed users
- supported platforms:
    - i.reddit
    - v.reddit
    - imgur (direct images, image posts, album posts)
    - gfycat
    - more? (pornhub, youtube, eroshare, erome, twitter, vidble)
- organize local files by user -> album -> post
    - want to include metadata (post permalink, post name/imgur title, date, etc), how to do this?
- keep a local db of what posts have been downloaded / last query time / etc?
- also go through comments for follow-up images?
- also go through self posts for links? or just download them as text files?
- download posts saved by user (sorted by subreddit)
*/

// TODO option to split a followed user's posts by subreddit
// TODO post karma thresholds
// TODO option to filter posts by NSFW
// TODO print how many bytes were downloaded (and set a limit)
// TODO option to explode 1-image albums to the top level
// TODO option to deduplicate identical url's posted multiple times

fun main() {
    val start = System.nanoTime()

    Config.load()

    val root = Path.of(System.getProperty("user.home"), "Pictures", "Reddit Downloads")
    val submissionSaver = SubmissionSaver(root = root)

    val results = linkedMapOf<Submission, SubmissionSaver.Result>()

    val followedUsers = reddit.followedUsers()
    followedUsers.forEachIndexed { userIndex, username ->
        println("Downloading posts by $username [${userIndex + 1} / ${followedUsers.size}]")

        reddit.user(username)
            .history("submitted")
            .limit(Paginator.RECOMMENDED_MAX_LIMIT)
            .sorting(UserHistorySort.TOP)
            .timePeriod(TimePeriod.ALL)
            .build()
            .take(1) // TODO temporary limit
            .forEachIndexed { listingIndex, listing ->
                val submissions = listing.children
                    .filterIsInstance(Submission::class.java)
                    .filter { !it.isStickied && !it.isSelfPost }

                println("  Got listing ${listingIndex + 1} (${submissions.size} submissions of ${listing.size} items)")
                submissions.forEach { submission ->
                    val result = submissionSaver.saveUserPost(submission)
                    results[submission] = result
                    when (result) {
                        is SubmissionSaver.Result.Saved ->
                            println("    Saved ${submission.redditUrl} to ${root.relativize(result.path)}")
                        is SubmissionSaver.Result.Failure ->
                            println("    [WARN] Unable to save ${submission.redditUrl} : ${result.message}")
                    }
                }
            }
    }

    val ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

    val saved = results.filterOfTypes<Submission, SubmissionSaver.Result.Saved>()
    val alreadySaved = results.filterOfTypes<Submission, SubmissionSaver.Result.AlreadySaved>()
    val ignored = results.filterOfTypes<Submission, SubmissionSaver.Result.Ignored>()
    val notFound = results.filterOfTypes<Submission, SubmissionSaver.Result.NotFound>()
    val unmatched = results.filterOfTypes<Submission, SubmissionSaver.Result.UnMatched>()
    val failures = results.filterOfTypes<Submission, SubmissionSaver.Result.Failure>()

    println()
    println("Done in %.4fs".format(ms / 1000.0))
    println("  ${saved.size + alreadySaved.size} successful: ${saved.size} new; ${alreadySaved.size} already saved")

    println("  ${ignored.size} ignored:")
    ignored.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${notFound.size} missing:")
    notFound.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${unmatched.size} with no media provider:")
    unmatched.forEach { println("    ${it.key.redditUrl} | ${it.key.url}") }

    println("  ${failures.size} failed:")
    failures.forEach {
        println("    ${it.key.redditUrl} | ${it.key.url} : ${it.value.message}")
    }

    System.exit(0)
}
