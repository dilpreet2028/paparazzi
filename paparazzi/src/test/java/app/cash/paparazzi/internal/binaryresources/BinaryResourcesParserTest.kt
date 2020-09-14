package app.cash.paparazzi.internal.binaryresources

import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.Test
import java.io.File

class BinaryResourcesParserTest {
    @Test
    fun testRawParsing() {
        BinaryResourcesParserTest::class.java.classLoader.getResource("resources.arsc").let {
            Assertions.assertThat(it).isNotNull()
            File(it!!.path)
        }.also {
            Assertions.assertThat(it)
                    .exists()
                    .canRead()
        }.let {
            it.inputStream()
        }.use {
            parseResourcesFile(it)
        }.also { parsedBinaryResources ->
            parsedBinaryResources.globalString.also { globalStrings ->
                Assert.assertEquals(8, globalStrings.stringsCount)
                Assert.assertEquals("1 2 3", globalStrings.getStringAtIndex(0))
                Assert.assertEquals("reg", globalStrings.getStringAtIndex(1))
                Assert.assertEquals("res/drawable/camera.png", globalStrings.getStringAtIndex(2))
                Assert.assertEquals("res/drawable/icn.xml", globalStrings.getStringAtIndex(3))
                Assert.assertEquals("res/drawable/icn2.xml", globalStrings.getStringAtIndex(4))
                Assert.assertEquals("res/layout/keypad.xml", globalStrings.getStringAtIndex(5))
                Assert.assertEquals("res/layout/launch.xml", globalStrings.getStringAtIndex(6))
                Assert.assertEquals("land", globalStrings.getStringAtIndex(7))
            }

            Assert.assertEquals(1, parsedBinaryResources.packages.size)
            parsedBinaryResources.packages[0].also { package1 ->
                Assert.assertSame(parsedBinaryResources.globalString, package1.globalStringsPool)

                package1.stringsPool.also { packageStrings ->
                    Assert.assertEquals(5, packageStrings.stringsCount)
                    Assert.assertEquals("color", packageStrings.getStringAtIndex(0))
                    Assert.assertEquals("drawable", packageStrings.getStringAtIndex(1))
                    Assert.assertEquals("id", packageStrings.getStringAtIndex(2))
                    Assert.assertEquals("layout", packageStrings.getStringAtIndex(3))
                    Assert.assertEquals("string", packageStrings.getStringAtIndex(4))
                }

                package1.keysPool.also { packageKeys ->
                    Assert.assertEquals(18, packageKeys.stringsCount)
                    Assert.assertEquals("bolt", packageKeys.getStringAtIndex(0))
                    Assert.assertEquals("cameraBody", packageKeys.getStringAtIndex(1))
                    Assert.assertEquals("cameraBodyAlpha", packageKeys.getStringAtIndex(2))
                    Assert.assertEquals("cameraBodyLow", packageKeys.getStringAtIndex(3))
                    Assert.assertEquals("cameraBodyLowAlpha", packageKeys.getStringAtIndex(4))
                    Assert.assertEquals("keypadDarkGrey", packageKeys.getStringAtIndex(5))
                    Assert.assertEquals("keypadGreen", packageKeys.getStringAtIndex(6))
                    Assert.assertEquals("launchBackground", packageKeys.getStringAtIndex(7))
                    Assert.assertEquals("camera", packageKeys.getStringAtIndex(8))
                    Assert.assertEquals("icn", packageKeys.getStringAtIndex(9))
                    Assert.assertEquals("icn2", packageKeys.getStringAtIndex(10))
                    Assert.assertEquals("amount", packageKeys.getStringAtIndex(11))
                    Assert.assertEquals("amount0", packageKeys.getStringAtIndex(12))
                    Assert.assertEquals("amount123", packageKeys.getStringAtIndex(13))
                    Assert.assertEquals("amount789", packageKeys.getStringAtIndex(14))
                    Assert.assertEquals("keypad", packageKeys.getStringAtIndex(15))
                    Assert.assertEquals("launch", packageKeys.getStringAtIndex(16))
                    Assert.assertEquals("orientation", packageKeys.getStringAtIndex(17))
                }

                Assert.assertEquals(5, package1.resources.size)
                Assert.assertEquals("color", package1.resources[0].resourceSpecName)
                Assert.assertEquals(1, package1.resources[0].configurations.size)
                Assert.assertEquals("color", package1.resources[0].configurations[0].resourceConfigurationName)
                package1.resources[0].configurations[0].configurationResources.let { colorResources ->
                    Assert.assertEquals(8, colorResources.size)
                    Assert.assertEquals("bolt", colorResources[0].resourceName)
                    Assert.assertEquals(1, colorResources[0].values.size)
                    Assert.assertEquals("#ffebff00", colorResources[0].values[0])
                    Assert.assertEquals("cameraBody", colorResources[1].resourceName)
                    Assert.assertEquals(1, colorResources[1].values.size)
                    Assert.assertEquals("#ff332f21", colorResources[1].values[0])
                    Assert.assertEquals("cameraBodyAlpha", colorResources[2].resourceName)
                    Assert.assertEquals(1, colorResources[2].values.size)
                    Assert.assertEquals("#ab332221", colorResources[2].values[0])
                    Assert.assertEquals("cameraBodyLow", colorResources[3].resourceName)
                    Assert.assertEquals(1, colorResources[3].values.size)
                    Assert.assertEquals("#ff332222", colorResources[3].values[0])
                    Assert.assertEquals("cameraBodyLowAlpha", colorResources[4].resourceName)
                    Assert.assertEquals(1, colorResources[4].values.size)
                    Assert.assertEquals("#aa332222", colorResources[4].values[0])
                    Assert.assertEquals("keypadDarkGrey", colorResources[5].resourceName)
                    Assert.assertEquals(1, colorResources[5].values.size)
                    Assert.assertEquals("#ff595959", colorResources[5].values[0])
                    Assert.assertEquals("keypadGreen", colorResources[6].resourceName)
                    Assert.assertEquals(1, colorResources[6].values.size)
                    Assert.assertEquals("#ff00d015", colorResources[6].values[0])
                    Assert.assertEquals("launchBackground", colorResources[7].resourceName)
                    Assert.assertEquals(1, colorResources[7].values.size)
                    Assert.assertEquals("#ffdcdccc", colorResources[7].values[0])
                }

                Assert.assertEquals("drawable", package1.resources[1].resourceSpecName)
                Assert.assertEquals(1, package1.resources[1].configurations.size)
                package1.resources[1].configurations[0].configurationResources.also { drawableResources ->
                    Assert.assertEquals(3, drawableResources.size)
                    drawableResources[0].also { drawableValue ->
                        Assert.assertEquals("camera", drawableValue.resourceName)
                        Assert.assertEquals(1, drawableValue.values.size)
                        Assert.assertEquals("res/drawable/camera.png", drawableValue.values[0])
                    }
                    drawableResources[1].also { drawableValue ->
                        Assert.assertEquals("icn", drawableValue.resourceName)
                        Assert.assertEquals(1, drawableValue.values.size)
                        Assert.assertEquals("res/drawable/icn.xml", drawableValue.values[0])
                    }
                    drawableResources[2].also { drawableValue ->
                        Assert.assertEquals("icn2", drawableValue.resourceName)
                        Assert.assertEquals(1, drawableValue.values.size)
                        Assert.assertEquals("res/drawable/icn2.xml", drawableValue.values[0])
                    }
                }

                Assert.assertEquals("id", package1.resources[2].resourceSpecName)
                Assert.assertEquals(1, package1.resources[2].configurations.size)
                package1.resources[2].configurations[0].configurationResources.also { idResources ->
                    Assert.assertEquals(4, idResources.size)
                    idResources[0].also { idValue ->
                        Assert.assertEquals("amount", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("@+id/amount", idValue.values[0])
                    }
                    idResources[1].also { idValue ->
                        Assert.assertEquals("amount0", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("@+id/amount0", idValue.values[0])
                    }
                    idResources[2].also { idValue ->
                        Assert.assertEquals("amount123", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("@+id/amount123", idValue.values[0])
                    }
                    idResources[3].also { idValue ->
                        Assert.assertEquals("amount789", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("@+id/amount789", idValue.values[0])
                    }
                }

                Assert.assertEquals("layout", package1.resources[3].resourceSpecName)
                Assert.assertEquals(1, package1.resources[3].configurations.size)
                package1.resources[3].configurations[0].configurationResources.also { layouts ->
                    Assert.assertEquals(2, layouts.size)
                    layouts[0].also { idValue ->
                        Assert.assertEquals("keypad", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("res/layout/keypad.xml", idValue.values[0])
                    }
                    layouts[1].also { idValue ->
                        Assert.assertEquals("launch", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("res/layout/launch.xml", idValue.values[0])
                    }
                }

                Assert.assertEquals("string", package1.resources[4].resourceSpecName)
                Assert.assertEquals(2, package1.resources[4].configurations.size)
                package1.resources[4].configurations[0].configurationResources.also { strings ->
                    Assert.assertEquals(2, strings.size)
                    strings[0].also { idValue ->
                        Assert.assertEquals("amount123", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("1 2 3", idValue.values[0])
                    }
                    strings[1].also { idValue ->
                        Assert.assertEquals("orientation", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("reg", idValue.values[0])
                    }
                }
                package1.resources[4].configurations[1].configurationResources.also { strings ->
                    Assert.assertEquals(2, strings.size)
                    strings[0].also { idValue ->
                        Assert.assertEquals("", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("@null", idValue.values[0])
                    }
                    strings[1].also { idValue ->
                        Assert.assertEquals("orientation", idValue.resourceName)
                        Assert.assertEquals(1, idValue.values.size)
                        Assert.assertEquals("land", idValue.values[0])
                    }
                }
            }
        }
    }
}