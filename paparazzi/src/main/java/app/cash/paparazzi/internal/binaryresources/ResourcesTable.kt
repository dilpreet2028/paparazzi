package app.cash.paparazzi.internal.binaryresources

data class Configuration(val mcc: Short,
                         val mnc: Short,

                         val language: String,
                         val country: String,

                         val orientation: Byte,
                         val touchscreen: Byte,
                         val density: Short) {
    //https://github.com/aosp-mirror/platform_frameworks_base/blob/master/libs/androidfw/ResourceTypes.cpp#L3130
    //https://developer.android.com/ndk/reference/group/configuration
    fun toQualifierString(): String {
        return StringBuilder()
                .appendQualifierIf(mcc, "mcc%d") { it != 0.toShort() }
                .appendQualifierIf(mnc, "mnc%d") { it != 0.toShort() }
                .appendQualifierIf(orientation, {
                    when (it.toInt()) {
                        1 -> "port"
                        2 -> "land"
                        3 -> "square"
                        else -> error("Unknown orientation value $it")
                    }
                }) { it != 0.toByte() }
                .appendQualifierIf(density, {
                    when (it.toInt()) {
                        120 -> "ldpi"
                        160 -> "mdpi"
                        213 -> "tvdpi"
                        240 -> "hdpi"
                        320 -> "xhdpi"
                        480 -> "xxhdpi"
                        640 -> "xxxhdpi"
                        0xffff -> "nodpi"
                        0xfffe -> "anydpi"
                        else -> "${it}dpi"
                    }
                }) { it != 0.toShort() }
                .appendQualifierIf(touchscreen, {
                    when (it.toInt()) {
                        1 -> "notouch"
                        2 -> "stylus"
                        3 -> "finger"
                        else -> error("Unknown touchscreen value $it")
                    }
                }) { it != 0.toByte() }
                .toString()

    }

    private fun <T : Any> StringBuilder.appendQualifierIf(value: T, format: String, predicate: (T) -> Boolean): StringBuilder {
        return appendQualifierIf(value, { str -> String.format(format, str) }, predicate)
    }

    private fun <T : Any> StringBuilder.appendQualifierIf(value: T, mapper: (T) -> String, predicate: (T) -> Boolean): StringBuilder {
        if (predicate(value)) {
            appendQualifier(mapper(value))
        }
        return this
    }

    private fun StringBuilder.appendQualifier(value: Any): StringBuilder {
        if (isNotEmpty()) append('-')
        return append(value)
    }
}