package com.tiquionophist.reddit

import java.io.FileInputStream
import java.util.Properties

object Config {

    private val secrets = Properties()

    fun load() {
        try {
            FileInputStream("secrets.properties").use { secrets.load(it) }
        } catch (ex: Throwable) {
            println("Failed to load secrets configuration: ${ex.message}")
        }
    }

    fun getSecret(name: String): String? = secrets.getProperty(name)
}
