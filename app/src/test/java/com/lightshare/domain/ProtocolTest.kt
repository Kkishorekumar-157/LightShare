package com.lightshare.domain

import com.lightshare.domain.model.Manifest
import com.lightshare.domain.model.Packet
import com.lightshare.domain.model.PacketType
import com.lightshare.domain.protocol.*
import com.lightshare.util.TestSupport
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ProtocolTest {
    @Test fun base45_rfc9285_vectors() {
        assertEquals("BB8", Base45.encode("AB".toByteArray()))
        assertEquals("%69 VD92EX0", Base45.encode("Hello!!".toByteArray()))
        assertEquals("QED8WEX0", Base45.encode("ietf!".toByteArray()))
        assertEquals("ietf!", String(Base45.decode("QED8WEX0")!!))
    }

    @Test fun base45_roundtrip_all_lengths() {
        for (n in 0..300) {
            val d = TestSupport.randomBytes(n, n.toLong())
            assertArrayEquals(d, Base45.decode(Base45.encode(d)))
        }
    }

    @Test fun base45_rejects_garbage() {
        assertNull(Base45.decode("a"))        // lowercase not in alphabet
        assertNull(Base45.decode("ABCD"))     // length % 3 == 1
        assertNull(Base45.decode(":::"))      // value > 0xFFFF
    }

    private fun sample(type: PacketType, seq: Int, k: Int = 10, bs: Int = 64) =
        Packet(1, type, 0xDEADBEEF.toInt(), seq, k, bs, (k - 1) * bs + 5L, TestSupport.randomBytes(bs))

    @Test fun packet_roundtrip_source_and_repair() {
        for (p in listOf(sample(PacketType.SOURCE, 3), sample(PacketType.REPAIR, 10), sample(PacketType.REPAIR, 123456))) {
            val q = PacketCodec.decode(PacketCodec.encode(p))
            assertEquals(p.type, q.type); assertEquals(p.transferId, q.transferId); assertEquals(p.seq, q.seq)
            assertEquals(p.blockCount, q.blockCount); assertEquals(p.blockSize, q.blockSize)
            assertEquals(p.fileSize, q.fileSize); assertArrayEquals(p.payload, q.payload)
        }
    }

    @Test fun manifest_roundtrip() {
        val sha = TestSupport.sha256(byteArrayOf(1, 2, 3))
        val m = Manifest("résumé ✓.pdf", "application/pdf", 1000, sha, 4, 256)
        val p = PacketCodec.decode(PacketCodec.encode(m.toPacket(99)))
        val r = Manifest.fromPacket(p)!!
        assertEquals("résumé ✓.pdf", r.fileName); assertEquals("application/pdf", r.mimeType)
        assertEquals(1000L, r.fileSize); assertArrayEquals(sha, r.sha256)
    }

    @Test fun sanitize_name() {
        assertEquals("a_b_c.txt", Manifest.sanitizeName("a/b\\c.txt"))
        assertEquals("received_file", Manifest.sanitizeName("   "))
        assertTrue(Manifest.sanitizeName("x".repeat(500)).length <= Manifest.MAX_NAME_BYTES)
    }

    @Test fun crc_detects_every_single_bit_flip() {
        val bytes = PacketCodec.encode(sample(PacketType.REPAIR, 77))
        for (i in bytes.indices) for (bit in 0 until 8) {
            val c = bytes.copyOf(); c[i] = (c[i].toInt() xor (1 shl bit)).toByte()
            try { PacketCodec.decode(c); fail("flip at byte $i bit $bit accepted") } catch (_: PacketFormatException) {}
        }
    }

    @Test fun rejects_truncated_and_empty() {
        val bytes = PacketCodec.encode(sample(PacketType.SOURCE, 0))
        for (n in 0 until bytes.size) {
            try { PacketCodec.decode(bytes.copyOf(n)); fail("accepted $n bytes") } catch (_: PacketFormatException) {}
        }
    }

    @Test fun rejects_semantically_invalid_packets() {
        fun rejected(p: Packet) = try { PacketCodec.decode(PacketCodec.encode(p)); false } catch (_: PacketFormatException) { true }
        assertTrue(rejected(sample(PacketType.SOURCE, 10)))              // index >= k
        assertTrue(rejected(sample(PacketType.REPAIR, 5)))               // repair seq < k
        assertTrue(rejected(Packet(2, PacketType.SOURCE, 1, 0, 10, 64, 600, ByteArray(64)))) // version
        assertTrue(rejected(Packet(1, PacketType.SOURCE, 1, 0, 10, 64, 100, ByteArray(64)))) // size/k mismatch
        assertTrue(rejected(Packet(1, PacketType.SOURCE, 1, 0, 10, 64, 600, ByteArray(10)))) // payload size
    }

    @Test fun frame_codec_roundtrip_and_rejects_foreign_qr() {
        val b = TestSupport.randomBytes(100)
        assertArrayEquals(b, FrameCodec.fromQrText(FrameCodec.toQrText(b)))
        assertNull(FrameCodec.fromQrText("https://example.com"))
        assertNull(FrameCodec.fromQrText(""))
        assertNull(FrameCodec.fromQrText("LS1:"))
    }

    @Test fun sender_frames_fit_qr_limit_and_alphabet() {
        val s = TestSupport.session(TestSupport.randomBytes(5000), 256)
        repeat(50) {
            val f = s.nextFrame()
            assertTrue(f.text.length <= s.maxQrChars)
            assertTrue(f.text.all { c -> c in "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:" })
        }
    }
}
