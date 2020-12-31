package app.cash.paparazzi.gradle

import app.cash.paparazzi.gradle.ImageSubject.Companion.assertThat
import com.google.common.truth.Truth.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PaparazziPluginTest {
  private lateinit var gradleRunner: GradleRunner

  @Before
  fun setUp() {
    gradleRunner = GradleRunner.create()
        .withPluginClasspath()
  }

  @Test
  fun missingPlugin() {
    val fixtureRoot = File("src/test/projects/missing-plugin")

    val result = gradleRunner
        .withArguments("preparePaparazziDebugResourcesVerify", "--stacktrace")
        .runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":preparePaparazziDebugResourcesVerify")).isNull()
    assertThat(result.output).contains(
        "The Android Gradle library/application plugin must be applied before the Paparazzi plugin."
    )
  }

  @Test
  fun cacheable() {
    val fixtureRoot = File("src/test/projects/cacheable")

    //deleting any previous cache data from disk.
    fixtureRoot.resolve("build").deleteRecursively()
    fixtureRoot.resolve(".gradle").deleteRecursively()
    fixtureRoot.resolve("build-cache").deleteRecursively()

    val firstRun = gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--build-cache", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    with(firstRun.task(":preparePaparazziDebugResourcesRecord")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isNotEqualTo(FROM_CACHE)
    }

    fixtureRoot.resolve("build").deleteRecursively()

    val secondRun = gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--build-cache", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    with(secondRun.task(":preparePaparazziDebugResourcesRecord")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(FROM_CACHE)
    }
  }

  @Test
  fun notCachedVerifyAfterRecord() {
    val fixtureRoot = File("src/test/projects/cacheable")

    //deleting any previous cache data from disk.
    fixtureRoot.resolve("build").deleteRecursively()
    fixtureRoot.resolve(".gradle").deleteRecursively()
    fixtureRoot.resolve("build-cache").deleteRecursively()

    val firstRun = gradleRunner
            .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--build-cache", "--stacktrace")
            .runFixture(fixtureRoot) { build() }

    assertThat(firstRun.task(":preparePaparazziDebugResourcesVerify")).isNull()
    with(firstRun.task(":preparePaparazziDebugResourcesRecord")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isNotEqualTo(FROM_CACHE)
    }
    with(firstRun.task(":testDebugUnitTest")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    fixtureRoot.resolve("build").deleteRecursively()

    val secondRun = gradleRunner
            .withArguments("testDebugUnitTest", "-PPAPARAZZI_VERIFY", "--build-cache", "--stacktrace")
            .runFixture(fixtureRoot) { build() }

    assertThat(secondRun.task(":preparePaparazziDebugResourcesRecord")).isNull()
    with(secondRun.task(":preparePaparazziDebugResourcesVerify")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isNotEqualTo(FROM_CACHE)
    }
    with(secondRun.task(":testDebugUnitTest")) {
      assertThat(this).isNotNull()
      assertThat(this!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    fixtureRoot.resolve("build-cache").deleteRecursively()
  }

  @Test
  fun interceptViewEditMode() {
    val fixtureRoot = File("src/test/projects/edit-mode-intercept")

    gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }
  }

  @Test
  fun record() {
    val fixtureRoot = File("src/test/projects/record-mode")

    val result = gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

     assertThat(result.task(":testDebugUnitTest")).isNotNull()

     val snapshotsDir = File(fixtureRoot, "src/test/snapshots")

    val snapshot = File(snapshotsDir, "images/app.cash.paparazzi.plugin.test_RecordTest_record.png")
    assertThat(snapshot.exists()).isTrue()

    val snapshotWithLabel = File(snapshotsDir, "images/app.cash.paparazzi.plugin.test_RecordTest_record_label.png")
    assertThat(snapshotWithLabel.exists()).isTrue()

    snapshotsDir.deleteRecursively()
  }

  @Test
  fun recordAllVariants() {
    val fixtureRoot = File("src/test/projects/record-mode")

    val result = gradleRunner
            .withArguments("recordPaparazzi", "--stacktrace")
            .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":recordPaparazziDebug")).isNotNull()
    assertThat(result.task(":recordPaparazziRelease")).isNotNull()

    val snapshotsDir = File(fixtureRoot, "src/test/snapshots")
    snapshotsDir.deleteRecursively()
  }

  @Test
  fun recordMultiModuleProject() {
    val fixtureRoot = File("src/test/projects/record-mode-multiple-modules")
    val moduleRoot = File(fixtureRoot, "module")

    val result = gradleRunner
        .withArguments("module:testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot, moduleRoot) { build() }

    assertThat(result.task(":module:testDebugUnitTest")).isNotNull()

    val snapshotsDir = File(moduleRoot, "src/test/snapshots")

    val snapshot = File(snapshotsDir, "images/app.cash.paparazzi.plugin.test_RecordTest_record.png")
    assertThat(snapshot.exists()).isTrue()

    val snapshotWithLabel = File(snapshotsDir, "images/app.cash.paparazzi.plugin.test_RecordTest_record_label.png")
    assertThat(snapshotWithLabel.exists()).isTrue()

    snapshotsDir.deleteRecursively()
  }

  @Test
  fun verifySuccess() {
    val fixtureRoot = File("src/test/projects/verify-mode-success")

    val result = gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_VERIFY", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":testDebugUnitTest")).isNotNull()
  }

  @Test
  fun verifyAllVariants() {
    val fixtureRoot = File("src/test/projects/verify-mode-success")

    val result = gradleRunner
        .withArguments("verifyPaparazzi", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":verifyPaparazziDebug")).isNotNull()
    assertThat(result.task(":verifyPaparazziRelease")).isNotNull()
  }

  @Test
  fun verifyFailure() {
    val fixtureRoot = File("src/test/projects/verify-mode-failure")

    val result = gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_VERIFY", "--stacktrace")
        .runFixture(fixtureRoot) { buildAndFail() }

    assertThat(result.task(":testDebugUnitTest")).isNotNull()

    val failureDir = File(fixtureRoot, "out/failures")
    val delta = File(failureDir, "delta-app.cash.paparazzi.plugin.test_VerifyTest_verify.png")
    assertThat(delta.exists()).isTrue()

    val goldenImage = File(fixtureRoot, "src/test/resources/expected_delta.png")
    assertThat(delta).isSimilarTo(goldenImage).withDefaultThreshold()

    failureDir.deleteRecursively()
  }

  @Test
  fun verifySuccessMultiModule() {
    val fixtureRoot = File("src/test/projects/verify-mode-success-multiple-modules")
    val moduleRoot = File(fixtureRoot, "module")

    val result = gradleRunner
        .withArguments("module:verifyPaparazziDebug", "--stacktrace")
        .runFixture(fixtureRoot, moduleRoot) { build() }

    assertThat(result.task(":module:testDebugUnitTest")).isNotNull()
  }

  @Test
  fun verifyFailureMultiModule() {
    val fixtureRoot = File("src/test/projects/verify-mode-failure-multiple-modules")
    val moduleRoot = File(fixtureRoot, "module")

    val result = gradleRunner
        .withArguments("module:testDebugUnitTest", "--stacktrace", "-PPAPARAZZI_VERIFY")
        .runFixture(fixtureRoot, moduleRoot) { buildAndFail() }

    assertThat(result.task(":module:testDebugUnitTest")).isNotNull()

    val failureDir = File(moduleRoot, "out/failures")
    val delta = File(failureDir, "delta-app.cash.paparazzi.plugin.test_VerifyTest_verify.png")
    assertThat(delta.exists()).isTrue()

    val goldenImage = File(moduleRoot, "src/test/resources/expected_delta.png")
    assertThat(delta).isSimilarTo(goldenImage).withDefaultThreshold()

    failureDir.deleteRecursively()
  }

  @Test
  fun verifyResourcesGeneratedForJavaProject() {
    val fixtureRoot = File("src/test/projects/verify-resources-java")

    val result = gradleRunner
            .withArguments("assembleDebugUnitTest", "--stacktrace", "-PPAPARAZZI_VERIFY")
            .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":preparePaparazziDebugResourcesRecord")).isNull()
    assertThat(result.task(":preparePaparazziDebugResourcesVerify")).isNotNull()

    val resourcesFile = File(fixtureRoot, "build/intermediates/paparazzi/Debug/resources.txt")
    assertThat(resourcesFile.exists()).isTrue()

    val resourceFileContents = resourcesFile.readLines()
    assertThat(resourceFileContents[0]).isEqualTo("app.cash.paparazzi.plugin.test")
    assertThat(resourceFileContents[1]).endsWith(
            "src/test/projects/verify-resources-java/build/intermediates/res/merged/debug"
    )
    assertThat(resourceFileContents[4]).endsWith(
            "src/test/projects/verify-resources-java/build/intermediates/library_assets/debug/out"
    )
  }

  @Test
  fun verifyRecordResourcesGeneratedForJavaProject() {
    val fixtureRoot = File("src/test/projects/verify-resources-java")

    val result = gradleRunner
            .withArguments("assembleDebugUnitTest", "--stacktrace", "-PPAPARAZZI_RECORD")
            .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":preparePaparazziDebugResourcesRecord")).isNotNull()
    assertThat(result.task(":preparePaparazziDebugResourcesVerify")).isNull()

    val resourcesFile = File(fixtureRoot, "build/intermediates/paparazzi/Debug/resources.txt")
    assertThat(resourcesFile.exists()).isTrue()

    val resourceFileContents = resourcesFile.readLines()
    assertThat(resourceFileContents[0]).isEqualTo("app.cash.paparazzi.plugin.test")
    assertThat(resourceFileContents[1]).endsWith(
            "src/test/projects/verify-resources-java/build/intermediates/res/merged/debug"
    )
    assertThat(resourceFileContents[4]).endsWith(
            "src/test/projects/verify-resources-java/build/intermediates/library_assets/debug/out"
    )
  }

  @Test
  fun verifyTargetSdkIsDifferentFromCompileSdk() {
    val fixtureRoot = File("src/test/projects/verify-target-sdk-compile-sdk")

    val result = gradleRunner
        .withArguments("compileDebugUnitTestJavaWithJavac", "--stacktrace", "-PPAPARAZZI_VERIFY")
        .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":preparePaparazziDebugResourcesVerify")).isNotNull()

    val resourcesFile = File(fixtureRoot, "build/intermediates/paparazzi/Debug/resources.txt")
    assertThat(resourcesFile.exists()).isTrue()

    val resourceFileContents = resourcesFile.readLines()
    assertThat(resourceFileContents[2]).isEqualTo("27")
    assertThat(resourceFileContents[3]).endsWith(
        "/platforms/android-28/"
    )
  }

  @Test
  fun verifyTargetSdkIsSameAsCompileSdk() {
    val fixtureRoot = File("src/test/projects/verify-resources-java")

    val result = gradleRunner
        .withArguments("compileDebugUnitTestJavaWithJavac", "--stacktrace", "-PPAPARAZZI_VERIFY")
        .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":preparePaparazziDebugResourcesVerify")).isNotNull()

    val resourcesFile = File(fixtureRoot, "build/intermediates/paparazzi/Debug/resources.txt")
    assertThat(resourcesFile.exists()).isTrue()

    val resourceFileContents = resourcesFile.readLines()
    assertThat(resourceFileContents[2]).isEqualTo("28")
    assertThat(resourceFileContents[3]).endsWith(
        "/platforms/android-28/"
    )
  }

  @Test
  fun verifyResourcesGeneratedForKotlinProject() {
    val fixtureRoot = File("src/test/projects/verify-resources-kotlin")

    val result = gradleRunner
        .withArguments("assembleDebugUnitTest", "--stacktrace", "-PPAPARAZZI_VERIFY")
        .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":preparePaparazziDebugResourcesVerify")).isNotNull()

    val resourcesFile = File(fixtureRoot, "build/intermediates/paparazzi/Debug/resources.txt")
    assertThat(resourcesFile.exists()).isTrue()

    val resourceFileContents = resourcesFile.readLines()
    assertThat(resourceFileContents[0]).isEqualTo("app.cash.paparazzi.plugin.test")
    assertThat(resourceFileContents[1]).endsWith(
        "src/test/projects/verify-resources-kotlin/build/intermediates/res/merged/debug"
    )
    assertThat(resourceFileContents[4]).endsWith(
        "src/test/projects/verify-resources-kotlin/build/intermediates/library_assets/debug/out"
    )
  }

  @Test
  fun verifySnapshot_withoutFonts() {
    val fixtureRoot = File("src/test/projects/verify-snapshot")

    val result = gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":preparePaparazziDebugResourcesRecord")).isNotNull()
    assertThat(result.task(":testDebugUnitTest")).isNotNull()

    val snapshotsDir = File(fixtureRoot, "build/reports/paparazzi/Debug/images")
    val snapshotFile = File(snapshotsDir, "8a7d289fef47bf8f177554eaa491fcfdf4fe1edf.png")
    assertThat(snapshotFile.exists()).isTrue()

    val goldenImage = File(fixtureRoot, "src/test/resources/launch_without_fonts.png")
    val actualFileBytes = Files.readAllBytes(snapshotFile.toPath())
    val expectedFileBytes = Files.readAllBytes(goldenImage.toPath())

    assertThat(actualFileBytes).isEqualTo(expectedFileBytes)
  }

  @Test
  @Ignore
  fun verifySnapshot() {
    val fixtureRoot = File("src/test/projects/verify-snapshot")

    val result = gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":preparePaparazziDebugResourcesRecord")).isNotNull()
    assertThat(result.task(":testDebugUnitTest")).isNotNull()

    val snapshotsDir = File(fixtureRoot, "build/reports/paparazzi/Debug/images")
    val snapshotFile = File(snapshotsDir, "06eed37f8377a96128efdbfd47e28b24ecac09e6.png")
    assertThat(snapshotFile.exists()).isTrue()

    val goldenImage = File(fixtureRoot, "src/test/resources/launch.png")
    val actualFileBytes = Files.readAllBytes(snapshotFile.toPath())
    val expectedFileBytes = Files.readAllBytes(goldenImage.toPath())

    assertThat(actualFileBytes).isEqualTo(expectedFileBytes)
  }

  @Test
  fun verifyAppSnapshot() {
    val fixtureRoot = File("src/test/projects/verify-snapshot-app")
    val goldenImage = File(fixtureRoot, "src/test/snapshots/images/app.cash.paparazzi.plugin.test_LaunchViewTest_testViews_launch.png")
    assertThat(goldenImage).exists()

    val result = gradleRunner
            .withArguments("testDebugUnitTest", "-PPAPARAZZI_VERIFY", "--stacktrace")
            .runFixture(fixtureRoot) { build() }

    assertThat(result.task(":preparePaparazziDebugResourcesVerify")).isNotNull()
    assertThat(result.task(":decodeDebugPaparazziApkResources")).isNotNull()
    assertThat(result.task(":testDebugUnitTest")).isNotNull()
    assertThat(result.task(":testDebugUnitTest")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun verifyVectorDrawables() {
    val fixtureRoot = File("src/test/projects/verify-svgs")

    gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    val snapshotsDir = File(fixtureRoot, "build/reports/paparazzi/Debug/images")
    val snapshots = snapshotsDir.listFiles()
    assertThat(snapshots!!).hasLength(1)

    val snapshotImage = snapshots[0]
    val goldenImage = File(fixtureRoot, "src/test/resources/arrow_up.png")
    assertThat(snapshotImage).isSimilarTo(goldenImage).withDefaultThreshold()
  }

  @Test
  fun withoutAppCompat() {
    val fixtureRoot = File("src/test/projects/appcompat-missing")

    gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    val snapshotsDir = File(fixtureRoot, "build/reports/paparazzi/Debug/images")
    val snapshotFile = File(snapshotsDir, "f76880c9f1f1fcae4d3cfcc870496b9abe3ee81b.png")
    assertThat(snapshotFile.exists()).isTrue()

    val goldenImage = File(fixtureRoot, "src/test/resources/arrow_missing.png")
    val actualFileBytes = Files.readAllBytes(snapshotFile.toPath())
    val expectedFileBytes = Files.readAllBytes(goldenImage.toPath())

    assertThat(actualFileBytes).isEqualTo(expectedFileBytes)
  }

  @Test
  fun withAppCompat() {
    val fixtureRoot = File("src/test/projects/appcompat-present")

    gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    val snapshotsDir = File(fixtureRoot, "build/reports/paparazzi/Debug/images")
    val snapshots = snapshotsDir.listFiles()
    assertThat(snapshots!!).hasLength(1)

    val snapshotImage = snapshots[0]
    val goldenImage = File(fixtureRoot, "src/test/resources/arrow_present.png")
    assertThat(snapshotImage).isSimilarTo(goldenImage).withDefaultThreshold()
  }

  @Test
  fun customFontsInXml() {
    val fixtureRoot = File("src/test/projects/custom-fonts-xml")

    gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    val snapshotsDir = File(fixtureRoot, "build/reports/paparazzi/Debug/images")
    val snapshots = snapshotsDir.listFiles()
    assertThat(snapshots!!).hasLength(1)

    val snapshotImage = snapshots[0]
    val goldenImage = File(fixtureRoot, "src/test/resources/textviews.png")
    assertThat(snapshotImage).isSimilarTo(goldenImage).withDefaultThreshold()
  }

  @Test
  fun customFontsInCode() {
    val fixtureRoot = File("src/test/projects/custom-fonts-code")

    gradleRunner
        .withArguments("testDebugUnitTest", "-PPAPARAZZI_RECORD", "--stacktrace")
        .runFixture(fixtureRoot) { build() }

    val snapshotsDir = File(fixtureRoot, "build/reports/paparazzi/Debug/images")
    val snapshots = snapshotsDir.listFiles()
    assertThat(snapshots!!).hasLength(1)

    val snapshotImage = snapshots[0]
    val goldenImage = File(fixtureRoot, "src/test/resources/textviews.png")
    assertThat(snapshotImage).isSimilarTo(goldenImage).withDefaultThreshold()
  }

  private fun GradleRunner.runFixture(
    projectRoot: File,
    moduleRoot: File = projectRoot,
    action: GradleRunner.() -> BuildResult
  ): BuildResult {
    val settings = File(projectRoot, "settings.gradle")
    if (!settings.exists()) {
      settings.createNewFile()
      settings.deleteOnExit()
    }

    val mainSourceRoot = File(moduleRoot, "src/main")
    val manifest = File(mainSourceRoot, "AndroidManifest.xml")
    if (!mainSourceRoot.exists() || !manifest.exists()) {
      mainSourceRoot.mkdirs()
      manifest.createNewFile()
      manifest.writeText("""<manifest package="app.cash.paparazzi.plugin.test"/>""")
      manifest.deleteOnExit()
    }

    val gradleProperties = File(projectRoot, "gradle.properties")
    if (!gradleProperties.exists()) {
      gradleProperties.createNewFile()
      gradleProperties.writeText("android.useAndroidX=true")
      gradleProperties.deleteOnExit()
    }

    return withProjectDir(projectRoot).action()
  }
}