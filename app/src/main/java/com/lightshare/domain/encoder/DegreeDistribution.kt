package com.lightshare.domain.encoder

/**
 * Robust Soliton degree distribution (Luby, 2002) with parameters c and delta.
 * StrictMath is used so results are bit-identical on every device. The table is truncated at
 * MAX_DEGREE (tail probability of ideal soliton beyond that is negligible) to bound memory for huge k.
 */
class DegreeDistribution(val k: Int, c: Double = 0.1, delta: Double = 0.5) {
    private val maxDegree = minOf(k, MAX_DEGREE)
    private val cdf = DoubleArray(maxDegree)

    init {
        require(k >= 1)
        val kd = k.toDouble()
        val r = maxOf(c * StrictMath.log(kd / delta) * StrictMath.sqrt(kd), 1.0)
        val spike = (kd / r).toInt().coerceIn(1, maxDegree)
        var total = 0.0
        val w = DoubleArray(maxDegree)
        for (d in 1..maxDegree) {
            val rho = if (d == 1) 1.0 / kd else 1.0 / (d.toDouble() * (d - 1).toDouble())
            val tau = when {
                d < spike -> r / (d.toDouble() * kd)
                d == spike -> r * StrictMath.log(r / delta) / kd
                else -> 0.0
            }
            w[d - 1] = rho + tau
            total += w[d - 1]
        }
        var acc = 0.0
        for (i in 0 until maxDegree) { acc += w[i] / total; cdf[i] = acc }
        cdf[maxDegree - 1] = 1.0
    }

    /** Maps u in [0,1) to a degree in 1..maxDegree. */
    fun sampleDegree(u: Double): Int {
        var lo = 0
        var hi = maxDegree - 1
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (cdf[mid] >= u) hi = mid else lo = mid + 1
        }
        return lo + 1
    }

    companion object { const val MAX_DEGREE = 50_000 }
}
