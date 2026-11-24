package com.lightshare.domain.decoder

import com.lightshare.domain.encoder.BlockSelector
import com.lightshare.domain.model.Manifest
import com.lightshare.domain.model.Packet
import com.lightshare.domain.model.PacketType
import com.lightshare.util.xorInto
import java.io.Closeable
import java.security.MessageDigest

enum class AcceptResult {
    /** New information stored (block recovered or equation queued). */
    ACCEPTED,
    MANIFEST,
    /** Same seq seen before. */
    DUPLICATE,
    /** Valid but every block it covers was already known. */
    REDUNDANT,
    /** Belongs to a different transfer than the one being received. */
    WRONG_TRANSFER,
    INVALID,
    /** Equation buffer full; packet discarded (may be re-accepted when it is seen again). */
    DROPPED,
    ALREADY_COMPLETE
}

enum class Verification { NOT_READY, OK, SIZE_MISMATCH, HASH_MISMATCH }

/**
 * Peeling (belief-propagation) LT decoder.
 *
 * Each repair packet is an equation "XOR of blocks {i1..id} = payload". Known blocks are XORed out;
 * an equation reduced to a single unknown yields that block, which is then propagated into all other
 * pending equations that mention it.
 *
 * LIMITATION: peeling can stall on a given set of packets even though a solution exists (a stopping
 * set); Gaussian elimination would solve some of those cases. We rely on the sender's endless supply
 * of fresh repair packets instead: a stall just means "keep scanning". No packet count is guaranteed
 * to suffice, only that decoding completes with overwhelming probability after ~k + O(sqrt(k)*ln^2(k/delta)).
 *
 * Recovered blocks are written to a [BlockStore] (temp file), so memory only holds pending equations
 * (bounded by [maxPendingEquations]). Not thread-safe: confine to one coroutine.
 */
class FountainDecoder(
    private val storeFactory: (blockCount: Int, blockSize: Int, transferId: Int) -> BlockStore,
    private val maxPendingEquations: Int = 100_000
) : Closeable {

    private class Equation(val remaining: HashSet<Int>, val payload: ByteArray)

    var transferId = 0; private set
    var blockCount = 0; private set
    var blockSize = 0; private set
    var fileSize = 0L; private set
    var manifest: Manifest? = null; private set
    var recoveredCount = 0; private set
    val isInitialized get() = store != null
    val isComplete get() = isInitialized && recoveredCount == blockCount
    val pendingEquations get() = equations.size
    val progress: Float get() = if (blockCount == 0) 0f else recoveredCount.toFloat() / blockCount
    val blockStore: BlockStore? get() = store

    private var store: BlockStore? = null
    private var selector: BlockSelector? = null
    private var recovered = BooleanArray(0)
    private var scratch = ByteArray(0)
    private val seen = HashSet<Int>()
    private val equations = HashMap<Int, Equation>()
    private val byIndex = HashMap<Int, MutableList<Int>>()

    fun accept(p: Packet): AcceptResult {
        if (!isInitialized) {
            transferId = p.transferId; blockCount = p.blockCount; blockSize = p.blockSize; fileSize = p.fileSize
            store = storeFactory(blockCount, blockSize, transferId)
            selector = BlockSelector(blockCount)
            recovered = BooleanArray(blockCount)
            scratch = ByteArray(blockSize)
        } else if (p.transferId != transferId || p.blockCount != blockCount ||
            p.blockSize != blockSize || p.fileSize != fileSize
        ) return AcceptResult.WRONG_TRANSFER

        if (p.type == PacketType.MANIFEST) {
            if (manifest != null) return AcceptResult.DUPLICATE
            manifest = Manifest.fromPacket(p) ?: return AcceptResult.INVALID
            return AcceptResult.MANIFEST
        }

        if (p.payload.size != blockSize) return AcceptResult.INVALID
        val indices: IntArray = when (p.type) {
            PacketType.SOURCE -> if (p.seq in 0 until blockCount) intArrayOf(p.seq) else return AcceptResult.INVALID
            else -> if (p.seq >= blockCount) selector!!.indicesFor(p.seq) else return AcceptResult.INVALID
        }
        if (isComplete) return AcceptResult.ALREADY_COMPLETE
        if (!seen.add(p.seq)) return AcceptResult.DUPLICATE

        val payload = p.payload.copyOf()
        val remaining = HashSet<Int>(indices.size * 2)
        for (idx in indices) {
            if (recovered[idx]) {
                store!!.read(idx, scratch)
                xorInto(payload, scratch)
            } else remaining.add(idx)
        }
        when (remaining.size) {
            0 -> return AcceptResult.REDUNDANT
            1 -> recoverAndPropagate(remaining.first(), payload)
            else -> {
                if (equations.size >= maxPendingEquations) { seen.remove(p.seq); return AcceptResult.DROPPED }
                equations[p.seq] = Equation(remaining, payload)
                for (idx in remaining) byIndex.getOrPut(idx) { ArrayList() }.add(p.seq)
            }
        }
        return AcceptResult.ACCEPTED
    }

    private fun recoverAndPropagate(startIdx: Int, startData: ByteArray) {
        val queue = ArrayDeque<Pair<Int, ByteArray>>()
        queue.addLast(startIdx to startData)
        while (queue.isNotEmpty()) {
            val (idx, data) = queue.removeFirst()
            if (recovered[idx]) continue
            store!!.write(idx, data)
            recovered[idx] = true
            recoveredCount++
            val ids = byIndex.remove(idx) ?: continue
            for (id in ids) {
                val eq = equations[id] ?: continue
                eq.remaining.remove(idx)
                xorInto(eq.payload, data)
                when (eq.remaining.size) {
                    0 -> equations.remove(id)
                    1 -> { equations.remove(id); queue.addLast(eq.remaining.first() to eq.payload) }
                }
            }
        }
    }

    fun isRecovered(index: Int) = index in recovered.indices && recovered[index]

    /** Fraction of recovered blocks in each of [cells] equal buckets (for progress maps). */
    fun coverage(cells: Int): FloatArray {
        val n = minOf(cells, blockCount)
        if (n <= 0) return FloatArray(0)
        val out = FloatArray(n)
        for (i in 0 until blockCount) if (recovered[i]) out[(i.toLong() * n / blockCount).toInt()] += 1f
        val cellSize = blockCount.toFloat() / n
        for (j in 0 until n) out[j] = (out[j] / cellSize).coerceAtMost(1f)
        return out
    }

    /** Trims the store to the exact file size, then checks size and SHA-256 against the manifest. */
    fun verify(): Verification {
        val m = manifest
        val s = store
        if (m == null || s == null || !isComplete) return Verification.NOT_READY
        if (m.fileSize != fileSize || s.finalizeLength(fileSize) != fileSize) return Verification.SIZE_MISMATCH
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(blockSize)
        var left = fileSize
        for (i in 0 until blockCount) {
            s.read(i, buf)
            val n = minOf(blockSize.toLong(), left).toInt()
            md.update(buf, 0, n)
            left -= n
        }
        return if (MessageDigest.isEqual(md.digest(), m.sha256)) Verification.OK else Verification.HASH_MISMATCH
    }

    override fun close() { store?.close(); equations.clear(); byIndex.clear() }
}
