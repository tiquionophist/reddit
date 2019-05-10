package com.tiquionophist.reddit

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * Determines whether the elements of this [List] satisfy the given [predicates], in order.
 *
 * If there are more elements in this [List] than [predicates], false is returned.
 * If there are more [predicates] than elements in this [List], the remaining [predicates] are tested against null.
 *
 * TODO this doesn't work for List<T?>
 */
fun <T> List<T>.satisfies(vararg predicates: (T?) -> Boolean): Boolean {
    if (size > predicates.size) return false

    predicates.forEachIndexed { index, predicate ->
        if (!predicate(getOrNull(index))) return false
    }

    return true
}

/**
 * Returns this [Map] filtered with keys of type [K] and values of type [V].
 */
inline fun <reified K, reified V> Map<*, *>.filterOfTypes(): Map<K, V> {
    @Suppress("UNCHECKED_CAST")
    return filter { it.key is K && it.value is V } as Map<K, V>
}

/**
 * Formats this [Duration] in a human-readable way.
 */
fun Duration.format(): String {
    val sb = StringBuilder()
    toDaysPart().takeIf { it > 0 }?.also {
        sb.append(it)
        sb.append("days ")
    }

    toHoursPart().takeIf { it > 0 }?.also {
        sb.append(it)
        sb.append("hr ")
    }

    toMinutesPart().takeIf { it > 0 }?.also {
        sb.append(it)
        sb.append("min ")
    }

    toSecondsPart().takeIf { it > 0 }?.also {
        sb.append(it)
        sb.append("sec ")
    }

    return sb.toString().trim()
}

/**
 * Returns a new [Path] equal to this [Path] but with the given [extension] appended.
 *
 * Note that [extension] should begin with the '.' character; it is not added automatically, and if this [Path] already
 * has an extension it is not removed before appending the new one.
 */
fun Path.withExtension(extension: String): Path {
    return resolveSibling(fileName.toString() + extension)
}

/**
 * Recursively deletes the given [path]; that is, if [path] is a directory its contents (and their contents,
 * recursively) will be deleted first.
 */
fun recursiveDelete(path: Path) {
    Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .forEach { Files.delete(it) }
}
