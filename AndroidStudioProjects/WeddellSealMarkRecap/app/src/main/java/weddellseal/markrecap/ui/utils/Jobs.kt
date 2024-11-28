package weddellseal.markrecap.ui.utils

import kotlinx.coroutines.Job

fun Set<Job>.cancelAll() {
    forEach { it.cancel() }
}

fun MutableSet<Job>.cancelAllAndClear() {
    cancelAll()
    clear()
}

fun Job.storeIn(set: MutableSet<Job>): Job {
    set.add(this)
    return this
}

fun mutableJobSet() = mutableSetOf<Job>()