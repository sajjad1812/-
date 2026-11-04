package com.iosexperience.app

/** Case-insensitive label search. Blank query returns everything. */
fun <T> filterByLabel(items: List<T>, query: String, label: (T) -> String): List<T> {
    val q = query.trim()
    if (q.isEmpty()) return items
    return items.filter { label(it).contains(q, ignoreCase = true) }
}
