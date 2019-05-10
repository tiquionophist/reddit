package com.tiquionophist.reddit

import net.dean.jraw.RedditClient
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.models.Submission
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import net.dean.jraw.pagination.Paginator

// TODO maybe just use the REST api directly since that's what we're doing with Gfycat and Imgur?

val reddit by lazy {
    // TODO move app name and version to config file
    val userAgent = UserAgent(
        platform = "jraw-bot",
        appId = "com.tiquionophist.reddit",
        version = "0.1",
        redditUsername = Config.getSecret("reddit.user.username").orEmpty()
    )

    val credentials = Credentials.script(
        username = Config.getSecret("reddit.user.username").orEmpty(),
        password = Config.getSecret("reddit.user.password").orEmpty(),
        clientId = Config.getSecret("reddit.client.id").orEmpty(),
        clientSecret = Config.getSecret("reddit.client.secret").orEmpty()
    )

    OAuthHelper.automatic(
        http = OkHttpNetworkAdapter(userAgent),
        creds = credentials
    ).apply {
        retryLimit = 2
        logHttp = false
    }
}

fun RedditClient.followedUsers(): List<String> {
    // TODO there should be a better way to get the list of followed users (but this seems to work fine)
    return me()
        .subreddits("subscriber")
        .limit(Paginator.RECOMMENDED_MAX_LIMIT)
        .build()
        .accumulateMerged(-1)
        .filter { it.name.startsWith("u_") }
        .map { it.name.substring(2) }
}

val Submission.metadata
    get() = Media.Metadata(id = id, author = author, date = created, title = title, subreddit = subreddit)

val Submission.redditUrl
    get() = "https://reddit.com$permalink"
