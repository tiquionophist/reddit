package com.tiquionophist.reddit

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object Config {
    private val settings = Properties()
    private val secrets = Properties()
    private var ignoredDomains: List<String> = emptyList()

    val followedUsers by lazy { getBooleanSetting("followedUsers") }
    val savedPosts by lazy { getBooleanSetting("savedPosts") }

    val saveNsfw by lazy { getBooleanSetting("nsfw") }
    val saveSfw by lazy { getBooleanSetting("sfw") }

    val explodeSingletonAlbums by lazy { getBooleanSetting("explodeSingletonAlbums") }

    val scoreThresholdSavedPost by lazy { getIntSetting("scoreThreshold.savedPost", Int.MIN_VALUE) }
    val scoreThresholdFollowedUser by lazy { getIntSetting("scoreThreshold.followedUser", Int.MIN_VALUE) }

    fun load() {
        try {
            FileInputStream("config/settings.properties").use { settings.load(it) }
        } catch (ex: FileNotFoundException) {
            println("Settings file not found: ${ex.message}")
        }

        try {
            FileInputStream("config/secrets.properties").use { secrets.load(it) }
        } catch (ex: FileNotFoundException) {
            println("Secrets file not found: ${ex.message}")
        }

        try {
            ignoredDomains = Files.readAllLines(Path.of("config/ignored_domains.txt"))
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.first() != '#' }
        } catch (ex: IOException) {
            println("Failed to load ignored list: ${ex.message}")
        }
    }

    private fun getBooleanSetting(name: String, default: Boolean = true): Boolean {
        return settings.getProperty(name)?.toBoolean() ?: default
    }

    private fun getIntSetting(name: String, default: Int): Int {
        return settings.getProperty(name)?.toIntOrNull() ?: default
    }

    fun getSecret(name: String): String? = secrets.getProperty(name)

    fun isIgnored(domain: String) = ignoredDomains.contains(domain)
}
