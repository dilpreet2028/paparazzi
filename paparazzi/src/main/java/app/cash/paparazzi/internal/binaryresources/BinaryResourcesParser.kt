package app.cash.paparazzi.internal.binaryresources

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private fun readChunkHeader(bytes: ByteBuffer): ResChunk {
    bytes.mark()
    val chunkType = bytes.short
    bytes.reset()

    return when(ResChunkType.fromChunkType(chunkType)) {
        ResChunkType.RES_TABLE_TYPE -> ResChunk.ResourceTableHeader(bytes)
        ResChunkType.RES_STRING_POOL_TYPE -> ResChunk.StringsPool(bytes)

        else -> error("chuckType $chunkType is unrecognizable.")
    }
}

internal fun readPackageChunk(bytes: ByteBuffer, globalStringsPool: ResChunk.StringsPool): ResChunk.PackageChunk {
    bytes.mark()
    val chunkType = bytes.short
    bytes.reset()

    return when(ResChunkType.fromChunkType(chunkType)) {
        ResChunkType.RES_TABLE_PACKAGE_TYPE -> ResChunk.PackageChunk(bytes, globalStringsPool)
        else -> error("chuckType $chunkType is unrecognizable.")
    }.also {
        it.moveToEndOfChunk()
    }
}

internal fun readChunkInPackageHeader(packageChunk: ResChunk.PackageChunk, bytes: ByteBuffer): ResChunk {
    bytes.mark()
    val chunkType = bytes.short
    bytes.reset()

    return when(ResChunkType.fromChunkType(chunkType)) {
        ResChunkType.RES_TABLE_TYPE_SPEC_TYPE -> ResChunk.TypeSpecChunk(packageChunk, bytes)
        ResChunkType.RES_TABLE_TYPE_TYPE -> ResChunk.TypeChunk(packageChunk, bytes)

        else -> error("chuckType $chunkType is unrecognizable for package-chunk.")
    }.also {
        it.moveToEndOfChunk()
    }
}

internal data class BinaryResources(val globalString: ResChunk.StringsPool, val packages: List<ResChunk.PackageChunk>)

internal fun parseResourcesFile(inputStream: InputStream): BinaryResources {
    ByteBuffer.wrap(inputStream.readBytes()).duplicate().also { it.order(ByteOrder.LITTLE_ENDIAN) }.run {
        val resourceTableHeader = readChunkHeader(this) as ResChunk.ResourceTableHeader
        val stringsPool = readChunkHeader(this) as ResChunk.StringsPool
        val packages = generateSequence(resourceTableHeader.packagesCount) { (it - 1).takeIf { it > 0 } }
            .map { readPackageChunk(this, stringsPool) }
            .toList()

        return BinaryResources(stringsPool, packages)
    }

}

