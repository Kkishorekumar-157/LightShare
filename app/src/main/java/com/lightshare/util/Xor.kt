package com.lightshare.util

/** dst[i] ^= src[i] for the first [len] bytes. */
fun xorInto(dst: ByteArray, src: ByteArray, len: Int = dst.size) {
    for (i in 0 until len) dst[i] = (dst[i].toInt() xor src[i].toInt()).toByte()
}
