package app.cash.paparazzi.internal.binaryresources

import android.util.TypedValue
import java.nio.ByteBuffer
import kotlin.experimental.and


/**
 * See https://github.com/aosp-mirror/platform_frameworks_base/blob/master/libs/androidfw/include/androidfw/ResourceTypes.h
 */

internal enum class ResChunkType(val chunkType: Short) {
    RES_NULL_TYPE(0x0000),
    RES_STRING_POOL_TYPE(0x0001),
    RES_TABLE_TYPE(0x0002),
    RES_XML_TYPE(0x0003),

    // Chunk types in RES_XML_TYPE
    RES_XML_FIRST_CHUNK_TYPE(0x0100),
    RES_XML_START_NAMESPACE_TYPE(0x0100),
    RES_XML_END_NAMESPACE_TYPE(0x0101),
    RES_XML_START_ELEMENT_TYPE(0x0102),
    RES_XML_END_ELEMENT_TYPE(0x0103),
    RES_XML_CDATA_TYPE(0x0104),
    RES_XML_LAST_CHUNK_TYPE(0x017f),

    // This contains a uint32_t array mapping strings in the string
    // pool back to resource identifiers.  It is optional.
    RES_XML_RESOURCE_MAP_TYPE(0x0180),

    // Chunk types in RES_TABLE_TYPE
    RES_TABLE_PACKAGE_TYPE(0x0200),
    RES_TABLE_TYPE_TYPE(0x0201),
    RES_TABLE_TYPE_SPEC_TYPE(0x0202),
    RES_TABLE_LIBRARY_TYPE(0x0203),
    RES_TABLE_OVERLAYABLE_TYPE(0x0204),
    RES_TABLE_OVERLAYABLE_POLICY_TYPE(0x0205);

    companion object {
        fun fromChunkType(chunkType: Short) =
                values().find { it.chunkType == chunkType }
                        ?: error("can not find enum value for $chunkType")
    }
}

//https://github.com/aosp-mirror/platform_frameworks_base/blob/master/libs/androidfw/include/androidfw/ResourceTypes.h#L264
internal enum class ResourceType(val type: Byte) {
    // The 'data' is either 0 or 1, specifying this resource is either
    // undefined or empty, respectively.
    TYPE_NULL(0x00),

    // The 'data' holds a ResTable_ref, a reference to another resource
    // table entry.
    TYPE_REFERENCE(0x01),

    // The 'data' holds an attribute resource identifier.
    TYPE_ATTRIBUTE(0x02),

    // The 'data' holds an index into the containing resource table's
    // global value string pool.
    TYPE_STRING(0x03),

    // The 'data' holds a single-precision floating point number.
    TYPE_FLOAT(0x04),

    // The 'data' holds a complex number encoding a dimension value,
    // such as "100in".
    TYPE_DIMENSION(0x05),

    // The 'data' holds a complex number encoding a fraction of a
    // container.
    TYPE_FRACTION(0x06),
    // The 'data' holds a dynamic ResTable_ref, which needs to be
    // resolved before it can be used like a TYPE_REFERENCE.
    /*TYPE_DYNAMIC_REFERENCE(0x07),*/
    // The 'data' holds an attribute resource identifier, which needs to be resolved
    // before it can be used like a TYPE_ATTRIBUTE.
    /*TYPE_DYNAMIC_ATTRIBUTE(0x08),*/

    // Beginning of integer flavors...
    /*TYPE_FIRST_INT(0x10),*/

    // The 'data' is a raw integer value of the form n..n.
    TYPE_INT_DEC(0x10),

    // The 'data' is a raw integer value of the form 0xn..n.
    TYPE_INT_HEX(0x11),

    // The 'data' is either 0 or 1, for input "false" or "true" respectively.
    TYPE_INT_BOOLEAN(0x12),

    // Beginning of color integer flavors...
    /*TYPE_FIRST_COLOR_INT(0x1c),*/

    // The 'data' is a raw integer value of the form #aarrggbb.
    TYPE_INT_COLOR_ARGB8(0x1c),

    // The 'data' is a raw integer value of the form #rrggbb.
    TYPE_INT_COLOR_RGB8(0x1d),

    // The 'data' is a raw integer value of the form #argb.
    TYPE_INT_COLOR_ARGB4(0x1e),

    // The 'data' is a raw integer value of the form #rgb.
    TYPE_INT_COLOR_RGB4(0x1f);

    // ...end of integer flavors.
    /*TYPE_LAST_COLOR_INT(0x1f),*/

    // ...end of integer flavors.
    /*TYPE_LAST_INT(0x1f);*/

    companion object {
        fun fromResourceType(type: Byte) =
                values().find { it.type == type }
                        ?: error("can not find enum value for $type")
    }
}

internal enum class ComplexUnit(val unitType: Byte) {
    // TYPE_DIMENSION: Value is raw pixels.
    COMPLEX_UNIT_PX(0),

    // TYPE_DIMENSION: Value is Device Independent Pixels.
    COMPLEX_UNIT_DIP(1),

    // TYPE_DIMENSION: Value is a Scaled device independent Pixels.
    COMPLEX_UNIT_SP(2),

    // TYPE_DIMENSION: Value is in points.
    COMPLEX_UNIT_PT(3),

    // TYPE_DIMENSION: Value is in inches.
    COMPLEX_UNIT_IN(4),

    // TYPE_DIMENSION: Value is in millimeters.
    COMPLEX_UNIT_MM(5);

    companion object {
        fun fromInt(type: Int): ComplexUnit {
            val byteValue = type.shl(COMPLEX_UNIT_SHIFT).and(COMPLEX_UNIT_MASK).toByte()
            return ComplexUnit.values().find { it.unitType == byteValue }
                    ?: error("can not find enum value for $type / $byteValue")
        }

        // Where the unit type information is.  This gives us 16 possible
        // types, as defined below.
        private const val COMPLEX_UNIT_SHIFT = 0
        private const val COMPLEX_UNIT_MASK = 0xf
    }
}

internal sealed class ResChunk(val chunkType: ResChunkType, protected val byteBuffer: ByteBuffer) {
    protected val headPosition: Int = byteBuffer.position()
    private val headerSize: Short
    protected val chunkSize: Int

    init {
        val chunkTypeFromBuffer = byteBuffer.short
        if (chunkTypeFromBuffer != chunkType.chunkType) error("byteBuffer is at the wrong position. It should be set to the first index of the chunk (type short). Expected ${chunkType.chunkType} but was ${chunkTypeFromBuffer}.")
        headerSize = byteBuffer.short
        chunkSize = byteBuffer.int
    }

    protected fun moveToEndOfHeader() {
        byteBuffer.position(headPosition + headerSize)
    }

    fun moveToEndOfChunk() {
        byteBuffer.position(headPosition + chunkSize)
    }

    class ResourceTableHeader(byteBuffer: ByteBuffer) : ResChunk(ResChunkType.RES_TABLE_TYPE, byteBuffer) {
        val packagesCount: Int = byteBuffer.int
    }

    class StringsPool(byteBuffer: ByteBuffer) : ResChunk(ResChunkType.RES_STRING_POOL_TYPE, byteBuffer) {
        private val strings: List<String>

        init {
            val stringCount = byteBuffer.int
            /*val styleCount =*/byteBuffer.int
            val flags = byteBuffer.int
            val stringsStart = byteBuffer.int
            /*val stylesStart = */byteBuffer.int

            //ensuring we're at the end of the header.
            moveToEndOfHeader()

            //reading the body

            /*val isSortedPool = flags.and(STRINGS_FLAG_SORTED) == STRINGS_FLAG_SORTED*/
            val isUtf8Pool = flags.and(STRINGS_FLAG_UTF8) == STRINGS_FLAG_UTF8

            strings = generateSequence(stringCount) { (it - 1).takeIf { it > 0 } }
                    //offsets are from the beginning of the chunk
                    .map { headPosition + stringsStart + byteBuffer.int }
                    .toList()//need to collect all offsets from the buffer first
                    .asSequence()
                    .map {
                        byteBuffer.position(it)
                        byteBuffer.readStringFromBuffer(isUtf8Pool)
                    }
                    .toList()

            //the next area in the chunk is the styles. Which we'll skip for now
            moveToEndOfChunk()
        }

        val stringsCount: Int
            get() = strings.size

        fun getStringAtIndex(index: Int) = strings.getOrElse(index) { "" }

        companion object {
            fun empty(): StringsPool =
                    ByteBuffer.allocate(32)
                            .putShort(ResChunkType.RES_STRING_POOL_TYPE.chunkType)
                            .putShort(8.toShort()/*header size*/)
                            .putInt(32/*chunk-size*/)
                            .putInt(0/*strings count*/)
                            .putInt(0/*styles count*/)
                            .putInt(0/*flags*/)
                            .putInt(0/*strings start*/)
                            .putInt(0/*styles start*/)
                            .let { StringsPool(it) }

            private const val STRINGS_FLAG_SORTED = 1
            private const val STRINGS_FLAG_UTF8 = 1.shl(8)
        }
    }

    class TypeSpecChunk(private val packageChunk: PackageChunk, byteBuffer: ByteBuffer) : ResChunk(ResChunkType.RES_TABLE_TYPE_SPEC_TYPE, byteBuffer) {
        val id: Byte
        val name: String
            get() = packageChunk.stringsPool.getStringAtIndex(id.toInt() - 1)
        val entriesCount: Int
        val entriesFlags: List<Int>

        init {
            id = byteBuffer.get()
            /*res0 = */byteBuffer.get()
            /*res1 = */byteBuffer.short
            entriesCount = byteBuffer.int
            entriesFlags = generateSequence(entriesCount) { (it - 1).takeIf { it > 0 } }
                    .map { byteBuffer.int }
                    .toList()
                    .toList()
        }
    }

    class TypeChunk(private val packageChunk: PackageChunk, byteBuffer: ByteBuffer) : ResChunk(ResChunkType.RES_TABLE_TYPE_TYPE, byteBuffer) {
        val id: Byte
        val name: String
            get() = packageChunk.stringsPool.getStringAtIndex(id.toInt() - 1)
        val entriesCount: Int

        //config
        val mcc: Short
        val mnc: Short

        val language: String
        val country: String

        val orientation: Byte
        val touchscreen: Byte
        val density: Short

        val typeValues: List<TypeEntry>

        init {
            id = byteBuffer.get()
            /*res0 = */byteBuffer.get()
            /*res1 = */byteBuffer.short
            entriesCount = byteBuffer.int
            val entriesStartOffset = byteBuffer.int

            /*val configSize = */byteBuffer.int

            mcc = byteBuffer.short
            mnc = byteBuffer.short

            language = byteBuffer.readUtf8String(2)
            country = byteBuffer.readUtf8String(2)

            orientation = byteBuffer.get()
            touchscreen = byteBuffer.get()
            density = byteBuffer.short

            //entries are at the end of the header
            moveToEndOfHeader()
            typeValues = generateSequence(entriesCount) { (it - 1).takeIf { it > 0 } }
                    //offsets are from the beginning of the chunk
                    .map { headPosition + entriesStartOffset + byteBuffer.int }
                    .toList()//need to collect all offsets from the buffer first
                    .map {
                        byteBuffer.position(it)
                        val typeStartPosition = byteBuffer.position()
                        val size = byteBuffer.short.and(8.or(16)/*ensuring it's 8 or 16 only*/)
                        val flags = byteBuffer.short
                        val keyRef = byteBuffer.int
                        val resourceName = packageChunk.keysPool.getStringAtIndex(keyRef)
                        val valuesCount = when {
                            size > 0 -> 0
                            TypeEntry.flagsHasComplexBit(flags) -> {
                                /*val parentId = */byteBuffer.int
                                byteBuffer.int
                            }
                            else -> 1
                        }
                        //moving to the start of the values list
                        byteBuffer.position(typeStartPosition + size)
                        val values = generateSequence(valuesCount) { (it - 1).takeIf { it > 0 } }
                                .map {
                                    val valueStartPosition = byteBuffer.position()
                                    val size = byteBuffer.short.and(8.or(16)/*ensuring it's 8 or 16 only*/)
                                    /*val res0 = */byteBuffer.get()
                                    when (ResourceType.fromResourceType(byteBuffer.get())) {
                                        ResourceType.TYPE_NULL -> "@null"
                                        ResourceType.TYPE_REFERENCE,
                                        ResourceType.TYPE_ATTRIBUTE -> "resourceId:0x${byteBuffer.int.toString(16)}"
                                        ResourceType.TYPE_STRING -> byteBuffer.int.let {
                                            if (it < 0) {
                                                "@null"
                                            } else {
                                                packageChunk.globalStringsPool.getStringAtIndex(it)
                                            }
                                        }
                                        ResourceType.TYPE_FLOAT -> Float.fromBits(byteBuffer.int).toString()
                                        ResourceType.TYPE_DIMENSION -> byteBuffer.int.let {
                                            val numberValue = TypedValue.complexToFloat(it)
                                            when (ComplexUnit.fromInt(it)) {
                                                ComplexUnit.COMPLEX_UNIT_PX -> "${numberValue}px"
                                                ComplexUnit.COMPLEX_UNIT_DIP -> "${numberValue}dp"
                                                ComplexUnit.COMPLEX_UNIT_SP -> "${numberValue}sp"
                                                ComplexUnit.COMPLEX_UNIT_PT -> "${numberValue}pt"
                                                ComplexUnit.COMPLEX_UNIT_IN -> "${numberValue}in"
                                                ComplexUnit.COMPLEX_UNIT_MM -> "${numberValue}mm"
                                            }
                                        }
                                        ResourceType.TYPE_FRACTION -> byteBuffer.int.let {
                                            "${Float.fromBits(it)}${if (it.and(0xf) == 0) "%" else "%p"}"
                                        }

                                        ResourceType.TYPE_INT_DEC -> byteBuffer.int.toString()
                                        ResourceType.TYPE_INT_HEX -> "0x${byteBuffer.int.toString(16)}"
                                        ResourceType.TYPE_INT_BOOLEAN -> {
                                            //this can be either an ID or a boolean (0 false, 1 true)
                                            if (name == "id") {
                                                "@+id/$resourceName"
                                            } else {
                                                (byteBuffer.int != 0).toString()
                                            }
                                        }
                                        ResourceType.TYPE_INT_COLOR_ARGB8 -> colorHex(byteBuffer.int, 8)
                                        ResourceType.TYPE_INT_COLOR_ARGB4 -> colorHex(byteBuffer.int, 4)
                                        ResourceType.TYPE_INT_COLOR_RGB8 -> colorHex(byteBuffer.int, 6)
                                        ResourceType.TYPE_INT_COLOR_RGB4 -> colorHex(byteBuffer.int, 3)
                                    }.also {
                                        byteBuffer.position(valueStartPosition + size)
                                    }
                                }
                                .toList()

                        TypeEntry(resourceName, flags, values)
                    }
                    .toList()
        }

        private fun colorHex(color: Int, bits: Int) = String.format("#%0${bits}x", color)


        private fun colorHexLow(color: Int, channels: Int) = generateSequence(channels - 1) { (it - 1).takeIf { it >= 0 } }
                .map { color.shr(4 * it).and(0xf) }
                .map { String.format("%x", it) }
                .joinToString(separator = "", prefix = "#")
    }

    //https://github.com/aosp-mirror/platform_frameworks_base/blob/master/libs/androidfw/include/androidfw/ResourceTypes.h#L872
    class PackageChunk(byteBuffer: ByteBuffer, val globalStringsPool: StringsPool) : ResChunk(ResChunkType.RES_TABLE_PACKAGE_TYPE, byteBuffer) {
        val id: Int
        val name: String
        internal val stringsPool: StringsPool
        internal val keysPool: StringsPool
        val resources: List<Resource>

        init {
            id = byteBuffer.int
            name = byteBuffer.readUtf16String(128)
            val typeStringsOffset = headPosition + byteBuffer.int
            /*val lastPublicTypeIndex = */byteBuffer.int
            val keyStringsOffset = headPosition + byteBuffer.int
            /*val lastPublicKeyStringIndex = */byteBuffer.int
            /*val typeIdOffset = */byteBuffer.int

            stringsPool = if (typeStringsOffset > 0) {
                byteBuffer.position(typeStringsOffset)
                StringsPool(byteBuffer)
            } else {
                StringsPool.empty()
            }
            keysPool = if (keyStringsOffset > 0) {
                byteBuffer.position(keyStringsOffset)
                StringsPool(byteBuffer)
            } else {
                StringsPool.empty()
            }

            val specs = mutableMapOf<Byte, TypeSpecChunk>()
            val types = mutableMapOf<Byte, MutableList<TypeChunk>>()
            while (byteBuffer.position() < headPosition + chunkSize) {
                when (val chunk = readChunkInPackageHeader(this, byteBuffer)) {
                    is TypeSpecChunk -> specs[chunk.id] = chunk
                    is TypeChunk -> types.getOrPut(chunk.id, { mutableListOf() }).add(chunk)
                    else -> error("Unexpected chunk-type ${chunk.chunkType}.")
                }
            }
            resources = specs
                    .map {
                        val spec = it.value
                        val entries = types[it.key]
                                ?: error("missing types list for spec id ${spec.id} / ${spec.name}.")
                        Resource(spec.name, entries.map {
                            ResourceConfigValue(it.name, it.mcc, it.mnc, it.language, it.country, it.orientation, it.touchscreen, it.density, it.typeValues)
                        }.toList())
                    }.toList()
        }
    }
}

data class TypeEntry(val resourceName: String, val flags: Short, val values: List<String>) {
    /**
     * if type-entry is not complex, there will only be one value.
     */
    val isComplex: Boolean
        get() = flagsHasComplexBit(flags)

    companion object {
        const val FLAG_COMPLEX = 0x0001.toShort()
        /*const val FLAG_PUBLIC = 0x0002.toShort()*/

        fun flagsHasComplexBit(flags: Short) = flags.and(FLAG_COMPLEX) == FLAG_COMPLEX
    }
}

data class ResourceConfigValue(val resourceConfigurationName: String, val mcc: Short, val mnc: Short, val language: String, val country: String, val orientation: Byte, val touchscreen: Byte, val density: Short,
                               val configurationResources: List<TypeEntry>)

data class Resource(val resourceSpecName: String, val configurations: List<ResourceConfigValue>)
