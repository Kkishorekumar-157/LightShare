package com.lightshare.domain.protocol

import com.lightshare.domain.model.Packet
import com.lightshare.domain.model.PacketType
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class PacketFormatException(message: String) : Exception(message)

/**
 * Binary packet layout (big-endian, protocol version 1):
 *
 *   u8      version            (= 1)
 *   u8      type               (0 manifest, 1 source, 2 repair)
 *   u32     transferId
 *   u32     seq                (source: block index; repair: >= blockCount; manifest: 0)
 *   varint  blockCount         (LEB128)
 *   varint  blockSize          (LEB128)
 *   varint  fileSize           (LEB128)
 *   bytes   payload            (source/repair: exactly blockSize bytes; manifest: <= 255 bytes)
 *   u32     CRC32 over every preceding byte
 *
 * Overhead for typical files: 1+1+4+4+3+2+3+4 = ~22 bytes.
 */
object PacketCodec {
    const val VERSION = 1
    const val MIN_BLOCK_SIZE = 16
    const val MAX_BLOCK_SIZE = 2048
    const val MAX_BLOCK_COUNT = 1 shl 22
    private const val MIN_PACKET = 2 + 4 + 4 + 3 + 4

    fun encode(p: Packet): ByteArray {
        val out = ByteArrayOutputStream(p.payload.size + 40)
        out.write(p.version)
        out.write(p.type.code)
        writeInt(out, p.transferId)
        writeInt(out, p.seq)
        writeVarint(out, p.blockCount.toLong())
        writeVarint(out, p.blockSize.toLong())
        writeVarint(out, p.fileSize)
        out.write(p.payload)
        val body = out.toByteArray()
        val crc = CRC32().also { it.update(body) }.value
        val res = body.copyOf(body.size + 4)
        for (i in 0 until 4) res[body.size + i] = (crc shr (24 - 8 * i)).toByte()
        return res
    }

    @Throws(PacketFormatException::class)
    fun decode(bytes: ByteArray): Packet {
        if (bytes.size < MIN_PACKET) throw PacketFormatException("too short")
        val end = bytes.size - 4
        var stored = 0L
        for (i in 0 until 4) stored = (stored shl 8) or (bytes[end + i].toLong() and 0xFF)
        val crc = CRC32().also { it.update(bytes, 0, end) }.value
        if (crc != stored) throw PacketFormatException("CRC mismatch")

        val r = Reader(bytes, end)
        val version = r.u8()
        if (version != VERSION) throw PacketFormatException("unsupported version $version")
        val type = PacketType.fromCode(r.u8()) ?: throw PacketFormatException("bad type")
        val tid = r.i32()
        val seq = r.i32()
        if (seq < 0) throw PacketFormatException("bad seq")
        val k = r.varint()
        val bs = r.varint()
        val size = r.varint()
        if (k < 1 || k > MAX_BLOCK_COUNT) throw PacketFormatException("bad block count")
        if (bs < MIN_BLOCK_SIZE || bs > MAX_BLOCK_SIZE) throw PacketFormatException("bad block size")
        if (size <= (k - 1) * bs || size > k * bs) throw PacketFormatException("size/blocks mismatch")
        val payload = bytes.copyOfRange(r.pos, end)
        when (type) {
            PacketType.MANIFEST -> {
                if (seq != 0) throw PacketFormatException("manifest seq")
                if (payload.size > 255 + 2 + 32) throw PacketFormatException("manifest size")
            }
            PacketType.SOURCE -> {
                if (seq >= k) throw PacketFormatException("source index out of range")
                if (payload.size != bs.toInt()) throw PacketFormatException("payload size")
            }
            PacketType.REPAIR -> {
                if (seq < k) throw PacketFormatException("repair seq in source range")
                if (payload.size != bs.toInt()) throw PacketFormatException("payload size")
            }
        }
        return Packet(version, type, tid, seq, k.toInt(), bs.toInt(), size, payload)
    }

    private fun writeInt(out: ByteArrayOutputStream, v: Int) {
        for (i in 3 downTo 0) out.write(v shr (8 * i))
    }

    private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (v >= 0x80) { out.write(((v and 0x7F) or 0x80).toInt()); v = v ushr 7 }
        out.write(v.toInt())
    }

    private class Reader(val b: ByteArray, val limit: Int) {
        var pos = 0
        fun u8(): Int { if (pos >= limit) throw PacketFormatException("truncated"); return b[pos++].toInt() and 0xFF }
        fun i32(): Int { var v = 0; repeat(4) { v = (v shl 8) or u8() }; return v }
        fun varint(): Long {
            var shift = 0; var v = 0L
            while (true) {
                val x = u8()
                v = v or ((x and 0x7F).toLong() shl shift)
                if (x and 0x80 == 0) return v
                shift += 7
                if (shift > 56) throw PacketFormatException("varint too long")
            }
        }
    }
}
