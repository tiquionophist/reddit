package com.tiquionophist.reddit

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object Config {

    /**
     * Whether to place the contents of singleton albums (albums with one image) at the level above.
     *
     * TODO move to a config file
     */
    const val bumpSingletons = true

    private val secrets = Properties()
    private var ignoredDomains: List<String> = emptyList()

    fun load() {
        try {
            FileInputStream("secrets.properties").use { secrets.load(it) }
        } catch (ex: FileNotFoundException) {
            println("Secrets configuration not found: ${ex.message}")
        }

        try {
            ignoredDomains = Files.readAllLines(Path.of("ignored_domains.txt"))
                .map { it.trim() }
                .filter { it.isNotBlank() && it.first() != '#' }
        } catch (ex: IOException) {
            println("Failed to load ignored list: ${ex.message}")
        }
    }

    fun getSecret(name: String): String? = secrets.getProperty(name)

    fun isIgnored(domain: String) = ignoredDomains.contains(domain)
}
