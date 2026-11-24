package com.lightshare.domain.model

enum class PacketType(val code: Int) {
    MANIFEST(0), SOURCE(1), REPAIR(2);

    companion object {
        fun fromCode(code: Int): PacketType? = entries.firstOrNull { it.code == code }
    }
}

/**
 * One protocol packet (before serialization).
 *  - MANIFEST: seq == 0, payload = manifest fields
 *  - SOURCE:   seq in [0, blockCount), payload = source block [seq]
 *  - REPAIR:   seq >= blockCount, payload = XOR of blocks chosen deterministically from seq
 */
class Packet(
    val version: Int,
    val type: PacketType,
    val transferId: Int,
    val seq: Int,
    val blockCount: Int,
    val blockSize: Int,
    val fileSize: Long,
    val payload: ByteArray
)
