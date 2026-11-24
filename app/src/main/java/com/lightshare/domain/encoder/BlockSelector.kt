package com.lightshare.domain.encoder

/**
 * Deterministic mapping seq -> set of source block indices for repair packets.
 * Encoder and decoder both call this, which is what makes the code decodable:
 * the packet carries only its seq, never the index list.
 */
class BlockSelector(val k: Int) {
    private val distribution = DegreeDistribution(k)

    fun indicesFor(seq: Int): IntArray {
        val rng = SplitMix64((seq.toLong() shl 32) xor (k.toLong() and 0xFFFFFFFFL) xor 0x4C53L)
        rng.nextLong() // warm-up
        val degree = minOf(distribution.sampleDegree(rng.nextDouble()), k)
        if (degree == k) return IntArray(k) { it }
        val chosen = HashSet<Int>(degree * 2)
        while (chosen.size < degree) chosen.add(rng.nextInt(k))
        return chosen.toIntArray().also { it.sort() }
    }
}
