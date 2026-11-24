package com.lightshare.domain.encoder

import com.lightshare.domain.model.Packet
import com.lightshare.domain.model.PacketType
import com.lightshare.domain.protocol.PacketCodec
import com.lightshare.util.xorInto

/**
 * Systematic LT encoder.
 *  seq in [0, k)  -> SOURCE packet carrying block seq verbatim
 *  seq >= k       -> REPAIR packet = XOR of the blocks BlockSelector chooses for seq
 * Stateless per call (except scratch buffers), so any seq can be generated in any order.
 * Not thread-safe.
 */
class FountainEncoder(private val source: BlockSource, val transferId: Int) {
    val blockCount = source.blockCount
    val blockSize = source.blockSize
    val fileSize = source.fileSize
    private val selector = BlockSelector(blockCount)
    private val scratch = ByteArray(blockSize)

    fun packetFor(seq: Int): Packet {
        require(seq >= 0)
        return if (seq < blockCount) {
            val buf = ByteArray(blockSize)
            source.readBlock(seq, buf)
            Packet(PacketCodec.VERSION, PacketType.SOURCE, transferId, seq, blockCount, blockSize, fileSize, buf)
        } else {
            val acc = ByteArray(blockSize)
            for (idx in selector.indicesFor(seq)) {
                source.readBlock(idx, scratch)
                xorInto(acc, scratch)
            }
            Packet(PacketCodec.VERSION, PacketType.REPAIR, transferId, seq, blockCount, blockSize, fileSize, acc)
        }
    }
}
