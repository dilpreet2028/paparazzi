package app.cash.paparazzi.binary

import org.junit.Assert
import org.junit.Test

class ParsersTests {
    private fun getAndroidRStream() = ResourceIdentifier::class.java.classLoader.getResourceAsStream("android.R.v30.jar")!!
    @Test
    fun testHappyPathEntireResourcesList() {
        val parsedResources = ParsersTests::class.java.classLoader.getResourceAsStream("resources.arsc")?.use {
            parseResourcesArsc(it, getAndroidRStream())
                    .let { arsc ->
                        arsc.resourcesMap.entries.joinToString(separator = System.lineSeparator()) { (id, valuesMap) ->
                            StringBuilder("- resource ")
                                    .append(id.resourceString).append(" ")
                                    .append("0x").append(id.id.toString(16))
                                    .appendLine(":").also { builder ->
                                        valuesMap.entries.forEach {
                                            builder.append("    * ").append("configuration ").append(it.key).appendLine(":")
                                            builder.append("      located under ").appendLine(it.value.resourceFilePath())
                                            if (!it.value.isCompressedFile()) {
                                                builder.append("        value ").appendLine(it.value.value().toXmlRepresentation(it.value, arsc))
                                            }
                                        }
                                    }
                        }
                    }
        } ?: error("resources.arsc was not found")

        val expect = ParsersTests::class.java.classLoader.getResourceAsStream("resources-dump.txt")?.use {
            it.reader().readText()
        }

        Assert.assertEquals(expect, parsedResources)
    }

    @Test
    fun testHappyPathValues() {
        val dumpedFileContents = ParsersTests::class.java.classLoader.getResourceAsStream("resources.arsc")?.use {
            parseResourcesArsc(it, getAndroidRStream())
                    .let { arsc -> valuesDump(arsc, encodedValues(arsc.resourcesMap)) }
                    .entries.joinToString(separator = System.lineSeparator()) { (path, fileContent) ->
                        StringBuilder("path ").append(path)
                                .appendLine(":")
                                .append(fileContent)

                    }
        } ?: error("resources.arsc was not found")

        val expect = ParsersTests::class.java.classLoader.getResourceAsStream("values-dump.txt")?.use {
            it.reader().readText()
        }

        Assert.assertEquals(expect, dumpedFileContents)
    }

    @Test
    fun testParseXmlFile() {
        val decodedXml = ParsersTests::class.java.classLoader.getResourceAsStream("binary_encoded_launch.xml")?.use { encodedXml ->
            ParsersTests::class.java.classLoader.getResourceAsStream("resources.arsc")?.use { arsc ->
                parseXmlFile(encodedXml, parseResourcesArsc(arsc, getAndroidRStream()))
            } ?: error("resources.arsc was not found")
        } ?: error("binary_encoded_launch.xml was not found")

        val expect = ParsersTests::class.java.classLoader.getResourceAsStream("decoded_launch.xml")?.use {
            it.reader().readText()
        }

        Assert.assertEquals(expect, decodedXml)
    }

    @Test
    fun testParseAndroidRClass() {
        val androidResourcesMap = parseRClass(getAndroidRStream())

        Assert.assertEquals(2547, androidResourcesMap.keys.size)
        Assert.assertEquals("?android:attr/absListViewStyle", androidResourcesMap[16842858]?.resourceString)
        Assert.assertEquals("android:absListViewStyle", androidResourcesMap[16842858]?.itemName)
        Assert.assertEquals("@android:string/ok", androidResourcesMap[17039370]?.resourceString)
        Assert.assertEquals("@android:style/Animation", androidResourcesMap[16973824]?.resourceString)
        Assert.assertEquals("android:Animation", androidResourcesMap[16973824]?.itemName)
    }
}