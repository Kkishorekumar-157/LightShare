package com.lightshare.domain.encoder

/** Explicit, platform-independent PRNG so sender and receiver derive identical block selections. */
class SplitMix64(seed: Long) {
    private var s = seed

    fun nextLong(): Long {
        s += GAMMA
        var z = s
        z = (z xor (z ushr 30)) * M1
        z = (z xor (z ushr 27)) * M2
        return z xor (z ushr 31)
    }

    fun nextDouble(): Double = (nextLong() ushr 11).toDouble() / (1L shl 53).toDouble()

    fun nextInt(bound: Int): Int = ((nextLong() ushr 1) % bound).toInt()

    private companion object {
        val GAMMA = 0x9E3779B97F4A7C15uL.toLong()
        val M1 = 0xBF58476D1CE4E5B9uL.toLong()
        val M2 = 0x94D049BB133111EBuL.toLong()
    }
}
