package com.tiquionophist.reddit

/**
 * Represents the sources of [net.dean.jraw.models.Submission]s that can be saved locally.
 *
 * These types are used to determine the [LocalLocation] of saved posts.
 */
enum class SubmissionType { SAVED_POST, FOLLOWED_USER }
