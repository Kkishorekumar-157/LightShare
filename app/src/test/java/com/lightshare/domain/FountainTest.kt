package com.lightshare.domain

import com.lightshare.domain.decoder.AcceptResult
import com.lightshare.domain.decoder.MemoryBlockStore
import com.lightshare.domain.decoder.Verification
import com.lightshare.domain.encoder.BlockSelector
import com.lightshare.domain.encoder.ByteArrayBlockSource
import com.lightshare.domain.encoder.FountainEncoder
import com.lightshare.domain.model.Manifest
import com.lightshare.domain.model.PacketType
import com.lightshare.domain.protocol.PacketCodec
import com.lightshare.util.TestSupport
import org.junit.Assert.*
import org.junit.Test

class FountainTest {

    @Test fun chunking_pads_last_block_and_counts() {
        val d = TestSupport.randomBytes(1000)
        val src = ByteArrayBlockSource(d, 256)
        assertEquals(4, src.blockCount)
        val buf = ByteArray(256)
        src.readBlock(3, buf)
        assertArrayEquals(d.copyOfRange(768, 1000), buf.copyOf(232))
        assertTrue(buf.copyOfRange(232, 256).all { it == 0.toByte() })
        assertEquals(1, ByteArrayBlockSource(ByteArray(1), 256).blockCount)
        assertEquals(1, ByteArrayBlockSource(ByteArray(256), 256).blockCount)
        assertEquals(2, ByteArrayBlockSource(ByteArray(257), 256).blockCount)
    }

    @Test fun selector_is_deterministic_distinct_and_in_range() {
        val a = BlockSelector(500); val b = BlockSelector(500)
        for (seq in 500 until 3000) {
            val x = a.indicesFor(seq)
            assertArrayEquals(x, b.indicesFor(seq))
            assertTrue(x.isNotEmpty() && x.all { it in 0 until 500 })
            assertEquals(x.size, x.toSet().size)
        }
    }

    @Test fun degree_distribution_has_low_degrees_and_a_spike() {
        val s = BlockSelector(1000)
        val degrees = (1000 until 6000).map { s.indicesFor(it).size }
        assertTrue("degree-1/2 should be common", degrees.count { it <= 2 } > degrees.size / 4)
        assertTrue("average degree stays small", degrees.average() < 40)
    }

    @Test fun repair_packet_is_xor_of_selected_blocks() {
        val d = TestSupport.randomBytes(3000)
        val src = ByteArrayBlockSource(d, 128)
        val enc = FountainEncoder(src, 1)
        val seq = src.blockCount + 17
        val p = enc.packetFor(seq)
        assertEquals(PacketType.REPAIR, p.type)
        val expect = ByteArray(128); val buf = ByteArray(128)
        for (i in BlockSelector(src.blockCount).indicesFor(seq)) { src.readBlock(i, buf); for (j in 0 until 128) expect[j] = (expect[j].toInt() xor buf[j].toInt()).toByte() }
        assertArrayEquals(expect, p.payload)
    }

    @Test fun end_to_end_small_to_large() {
        for (size in listOf(1, 15, 255, 256, 257, 1000, 10_000, 100_000, 1_000_000)) {
            val data = TestSupport.randomBytes(size, size.toLong())
            val o = TestSupport.transfer(data, 256)
            assertEquals("size $size", Verification.OK, o.verification)
            assertArrayEquals("size $size", data, o.data)
        }
    }

    @Test fun lossless_in_order_needs_about_k_frames() {
        val data = TestSupport.randomBytes(256 * 200)
        val o = TestSupport.transfer(data, 256)
        assertTrue("frames=${o.framesGenerated}", o.framesGenerated <= 200 + 200 / 8 + 5)
    }

    @Test fun recovers_from_30_percent_loss() {
        val data = TestSupport.randomBytes(60_000)
        val o = TestSupport.transfer(data, 256, dropRate = 0.30)
        assertEquals(Verification.OK, o.verification); assertArrayEquals(data, o.data)
    }

    @Test fun recovers_from_heavy_loss_70_percent() {
        val data = TestSupport.randomBytes(30_000)
        val o = TestSupport.transfer(data, 256, dropRate = 0.70)
        assertEquals(Verification.OK, o.verification); assertArrayEquals(data, o.data)
    }

    @Test fun recovers_with_shuffled_order_and_loss() {
        val data = TestSupport.randomBytes(80_000)
        val o = TestSupport.transfer(data, 256, dropRate = 0.2, shuffleWindow = 64, seed = 99)
        assertEquals(Verification.OK, o.verification); assertArrayEquals(data, o.data)
    }

    @Test fun repair_packets_alone_can_rebuild_the_file() {
        // Late joiner who missed the whole systematic pass.
        val data = TestSupport.randomBytes(256 * 300)
        val s = TestSupport.session(data, 256)
        val dec = TestSupport.decoder()
        var used = 0
        var sentManifest = false
        while (!(dec.isComplete && dec.manifest != null)) {
            val f = s.nextFrame()
            if (f.type == PacketType.SOURCE) continue
            if (f.type == PacketType.MANIFEST && sentManifest) continue
            sentManifest = sentManifest || f.type == PacketType.MANIFEST
            dec.accept(PacketCodec.decode(com.lightshare.domain.protocol.FrameCodec.fromQrText(f.text)!!)); used++
            assertTrue("did not converge, used=$used", used < 300 * 8)
        }
        println("repair-only decode of 300 blocks used $used packets (${"%.2f".format(used / 300.0)}x)")
        assertEquals(Verification.OK, dec.verify())
    }

    @Test fun duplicates_wrong_transfer_and_invalid_are_handled() {
        val data = TestSupport.randomBytes(2000)
        val s = TestSupport.session(data, 256)
        val dec = TestSupport.decoder()
        fun pk(text: String) = PacketCodec.decode(com.lightshare.domain.protocol.FrameCodec.fromQrText(text)!!)
        val manifest = pk(s.nextFrame().text)
        val src0 = pk(s.nextFrame().text)
        assertEquals(AcceptResult.MANIFEST, dec.accept(manifest))
        assertEquals(AcceptResult.DUPLICATE, dec.accept(manifest))
        assertEquals(AcceptResult.ACCEPTED, dec.accept(src0))
        assertEquals(AcceptResult.DUPLICATE, dec.accept(src0))
        assertEquals(1, dec.recoveredCount)

        val other = TestSupport.session(TestSupport.randomBytes(2000, 5), 256).let {
            // same geometry, different transfer id
            com.lightshare.domain.encoder.SenderSession(ByteArrayBlockSource(TestSupport.randomBytes(2000, 5), 256), "x", "a/b",
                TestSupport.sha256(ByteArray(1)), com.lightshare.domain.model.SenderConfig(blockSize = 256), transferId = 777)
        }
        other.nextFrame()
        assertEquals(AcceptResult.WRONG_TRANSFER, dec.accept(pk(other.nextFrame().text)))
        assertEquals(1, dec.recoveredCount)
    }

    @Test fun insufficient_packets_do_not_complete_and_progress_is_reported() {
        val data = TestSupport.randomBytes(256 * 50)
        val s = TestSupport.session(data, 256)
        val dec = TestSupport.decoder()
        repeat(20) { dec.accept(PacketCodec.decode(com.lightshare.domain.protocol.FrameCodec.fromQrText(s.nextFrame().text)!!)) }
        assertFalse(dec.isComplete)
        assertTrue(dec.progress > 0f && dec.progress < 1f)
        assertEquals(Verification.NOT_READY, dec.verify())
    }

    @Test fun sha256_mismatch_is_detected() {
        val data = TestSupport.randomBytes(5000)
        val enc = FountainEncoder(ByteArrayBlockSource(data, 256), 5)
        val dec = TestSupport.decoder()
        val wrongHash = TestSupport.sha256(ByteArray(3))
        dec.accept(Manifest("f", "x/y", data.size.toLong(), wrongHash, enc.blockCount, 256).toPacket(5))
        for (i in 0 until enc.blockCount) dec.accept(enc.packetFor(i))
        assertTrue(dec.isComplete)
        assertEquals(Verification.HASH_MISMATCH, dec.verify())
    }

    @Test fun corrupted_block_is_detected_by_hash() {
        val data = TestSupport.randomBytes(5000)
        val enc = FountainEncoder(ByteArrayBlockSource(data, 256), 5)
        val dec = TestSupport.decoder()
        dec.accept(Manifest("f", "x/y", data.size.toLong(), TestSupport.sha256(data), enc.blockCount, 256).toPacket(5))
        for (i in 0 until enc.blockCount) {
            val p = enc.packetFor(i)
            if (i == 7) p.payload[0] = (p.payload[0].toInt() xor 1).toByte() // bypasses CRC on purpose
            dec.accept(p)
        }
        assertEquals(Verification.HASH_MISMATCH, dec.verify())
    }
}
