package com.tiquionophist.reddit

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
    return filter { it.key is K && it.value is V } as Map<K, V>
}
