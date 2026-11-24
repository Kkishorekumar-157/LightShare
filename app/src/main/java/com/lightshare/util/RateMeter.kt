package com.lightshare.util

/** Sliding-window event/byte rate. Feed it real timestamps (ms); nothing is fabricated. */
class RateMeter(private val windowMs: Long = 5_000L) {
    private val times = ArrayDeque<Long>()
    private val sizes = ArrayDeque<Int>()
    private var byteSum = 0L

    fun record(nowMs: Long, bytes: Int = 0) {
        times.addLast(nowMs); sizes.addLast(bytes); byteSum += bytes
        prune(nowMs)
    }

    fun clear() { times.clear(); sizes.clear(); byteSum = 0 }

    /** Events per second over the window (0 until at least two events). */
    fun eventsPerSecond(nowMs: Long): Double {
        prune(nowMs)
        if (times.size < 2) return 0.0
        val span = (nowMs - times.first()).coerceAtLeast(1_000L)
        return times.size * 1000.0 / span
    }

    fun bytesPerSecond(nowMs: Long): Double {
        prune(nowMs)
        if (times.size < 2) return 0.0
        val span = (nowMs - times.first()).coerceAtLeast(1_000L)
        return byteSum * 1000.0 / span
    }

    private fun prune(nowMs: Long) {
        while (times.isNotEmpty() && nowMs - times.first() > windowMs) {
            times.removeFirst(); byteSum -= sizes.removeFirst()
        }
    }
}
