package com.tiquionophist.reddit

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.HttpUrl

/**
 * A wrapper around youtube-dl: https://github.com/ytdl-org/youtube-dl
 */
object YoutubeDl {
    sealed class Result {
        class Success(val extension: String, val bytes: Long) : Result()
        object Failure : Result()
    }

    private val runtime = Runtime.getRuntime()
    private val prefix = if (isWindows()) "cmd /c " else ""
    private val gson = Gson()

    val isInstalled by lazy { runtime.exec("${prefix}youtube-dl --version").waitFor() == 0 }

    fun download(url: HttpUrl, filename: String): Result {
        val process = runtime.exec("${prefix}youtube-dl $url --output \"$filename.%(ext)s\" --print-json --no-mtime")

        val output = process.inputStream.use { it.bufferedReader().readLines().joinToString(separator = " ").trim() }
        val exitVal = process.waitFor()

        return if (exitVal == 0) {
            try {
                val model = gson.fromJson(output, OutputModel::class.java)
                Result.Success(extension = "." + model.ext, bytes = model.filesize)
            } catch (ex: JsonSyntaxException) {
                Result.Failure
            }
        } else {
            Result.Failure
        }
    }

    private data class OutputModel(val ext: String, val filesize: Long)
}
