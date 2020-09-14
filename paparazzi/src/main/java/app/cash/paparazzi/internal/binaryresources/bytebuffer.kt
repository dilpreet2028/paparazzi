package app.cash.paparazzi.internal.binaryresources

import java.nio.ByteBuffer
import kotlin.experimental.and


internal fun ByteBuffer.readUtf8String(bytesLength: Int): String = ByteArray(bytesLength)
        .also { this.get(it) }
        .let { String(it, Charsets.UTF_8) }.trimEnd(0.toChar())

internal fun ByteBuffer.readUtf16String(charsLength: Int): String = CharArray(charsLength)
        .also {
            this.asCharBuffer().get(it)
            //moving the parent buffer position
            this.position(this.position() + charsLength * 2)
        }
        .let { String(it) }.trimEnd(0.toChar())

internal fun ByteBuffer.readStringFromBuffer(utf8Pool: Boolean): String {
    return if (utf8Pool) {
        val stringLength = this.readUtf8StringLength()
        val bytesLength = this.readUtf8StringLength()
        this.readUtf8String(bytesLength + 1 /*the trailing null*/)
                .also {
                    //verifications
                    if (it.length != stringLength) error("Expected string with length $stringLength but got ${it.length}. Bytes length is $bytesLength. String is '$it'.")
                }
    } else {
        val charsLength = this.read16StringLength()
        this.readUtf16String(charsLength + 1 /*the trailing null*/)
                .also {
                    //verifications
                    if (it.length != charsLength) error("Expected string with length $charsLength but got ${it.length}.")
                }
    }
}

internal fun ByteBuffer.getUByte(): Int = this.get().and(0xff.toByte()).toInt()
internal fun ByteBuffer.getUShort(): Int = this.short.and(0xffff.toShort()).toInt()

//https://github.com/aosp-mirror/platform_frameworks_base/blob/master/libs/androidfw/ResourceTypes.cpp#L705
internal fun ByteBuffer.readUtf8StringLength(): Int {
    val firstByte = this.get().toInt()

    if (firstByte.and(0x80) != 0) {
        //additional byte needed for length
        return firstByte
                .and(0x7f)
                .shl(8)
                .or(this.get().toInt())
    } else {
        return firstByte
    }
}

//https://github.com/aosp-mirror/platform_frameworks_base/blob/master/libs/androidfw/ResourceTypes.cpp#L683
internal fun ByteBuffer.read16StringLength(): Int {
    var length = this.getUShort()

    if (length.and(0x8000) != 0) {
        //additional short needed for length
        length = length.and(0x7fff).shl(16)
        length = length.or(this.getUShort())
    }

    return length
}