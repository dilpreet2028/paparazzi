/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi.gradle

import app.cash.paparazzi.VERSION
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import java.util.Locale

@Suppress("unused")
@OptIn(ExperimentalStdlibApi::class)
class PaparazziPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val hasLibraryPlugin = project.plugins.hasPlugin("com.android.library")
    val hasAppPlugin = project.plugins.hasPlugin("com.android.application")
    require(hasLibraryPlugin || hasAppPlugin) {
      "The Android Gradle library/application plugin must be applied before the Paparazzi plugin."
    }

    project.configurations.getByName("testImplementation").dependencies.add(
        project.dependencies.create("app.cash.paparazzi:paparazzi:$VERSION")
    )

    //Creating an root tasks for executing all snapshot tasks for all variants.
    //These tasks will depend on all variant-specific tasks.
    val verifyAllVariant = project.tasks.register("verifyPaparazzi")
    val recordAllVariant = project.tasks.register("recordPaparazzi")

    if (hasLibraryPlugin) {
      project.extensions.getByType(LibraryExtension::class.java)
          .libraryVariants.all {
              setupPaparazziTasks(project, it, it.unitTestVariant,
                  it.mergeResourcesProvider, it.mergeResourcesProvider.flatMap { it.outputDir },
                  it.mergeAssetsProvider, it.mergeAssetsProvider.flatMap { it.outputDir },
                  recordAllVariant, verifyAllVariant
              )
          }
    } else {
      project.extensions.getByType(AppExtension::class.java)
          .applicationVariants.all {
              val variantSlug = it.name.capitalize(Locale.US)
              //for apps, we'll need to extract the merged resources and decode them first
              val decodeApkTask = project.tasks.register(
                      "decode${variantSlug}PaparazziApkResources", DecodeApkResourcesTask::class.java
              ) { task ->
                task.apkDirectory.set( it.packageApplicationProvider.flatMap { it.outputDirectory })
              }
              setupPaparazziTasks(project, it, it.unitTestVariant,
                  decodeApkTask, decodeApkTask.flatMap { it.decodedResourcesDirectory },
                  decodeApkTask, decodeApkTask.flatMap { it.decodedAssetsDirectory },
                  recordAllVariant, verifyAllVariant
              )
          }
    }
  }

  private fun createResourcesWriterTask(project: Project, variantSlug: String, mode: String,
                                        resourcesTask: Provider<out Task>, resourcesProvider: Provider<Directory>,
                                        assetsTask: Provider<out Task>, assetsProvider: Provider<Directory>,
                                        reportOutputFolder: Provider<Directory>): TaskProvider<PrepareResourcesTask> {
    return project.tasks.register(
            "preparePaparazzi${variantSlug}Resources${mode.capitalize(Locale.ROOT)}", PrepareResourcesTask::class.java
    ) { task ->
      task.dependsOn(resourcesTask)
      task.mergeResourcesOutput.set(resourcesProvider)
      task.dependsOn(assetsTask)
      task.mergeAssetsOutput.set(assetsProvider)
      task.paparazziResources.set(project.layout.buildDirectory.file("intermediates/paparazzi/${variantSlug}/resources.txt"))
      task.paparazziReportOutput = reportOutputFolder.get().asFile.absolutePath
      task.paparazziMode = mode
    }
  }

  private fun configureTestTask(project: Project, testVariantSlug: String,
                                paparazziResourcesTask: TaskProvider<PrepareResourcesTask>,
                                reportOutputFolder: Provider<Directory>):
          TaskProvider<Test> {
    return project.tasks.named("test${testVariantSlug}", Test::class.java) { test ->
      test.systemProperties["paparazzi.test.resources"] =
              paparazziResourcesTask.flatMap { it.paparazziResources.asFile }.get().path
      //We're adding the report folder to the test-task's outputs to ensure
      //it is stored in Gradle's cache.
      test.outputs.dir(reportOutputFolder)
      test.dependsOn(paparazziResourcesTask)
      //ensuring that the test task knows about the resources output
      test.inputs.file(paparazziResourcesTask.flatMap { it.paparazziResources.asFile })
      test.doLast {
        val uri = reportOutputFolder.get().asFile.toPath().resolve("index.html").toUri()
        project.logger.log(LIFECYCLE, "See the Paparazzi report at: $uri")
      }
    }
  }

  private fun setupPaparazziTasks(project: Project, variant: BaseVariant, unitTestVariant: UnitTestVariant,
                                  resourcesTask: Provider<out Task>, resourcesProvider: Provider<Directory>,
                                  assetsTask: Provider<out Task>, assetsProvider: Provider<Directory>,
                                  recordAllVariant: TaskProvider<out Task>, verifyAllVariant: TaskProvider<out Task>) {
    val variantSlug = variant.name.capitalize(Locale.US)
    val reportOutputFolder = project.layout.buildDirectory.dir("reports/paparazzi/$variantSlug")
    val writeResourcesTask = createResourcesWriterTask(project, variantSlug, getPaparazziMode(project.rootProject),
            resourcesTask, resourcesProvider, assetsTask, assetsProvider, reportOutputFolder)

    val recordTaskProvider = project.tasks.register("recordPaparazzi${variantSlug}")
    recordAllVariant.configure {
      it.dependsOn(recordTaskProvider)
    }
    val verifyTaskProvider = project.tasks.register("verifyPaparazzi${variantSlug}")
    verifyAllVariant.configure {
      it.dependsOn(verifyTaskProvider)
    }

    val testVariantSlug = unitTestVariant.name.capitalize(Locale.US)

    when {
      project.plugins.hasPlugin(JavaBasePlugin::class.java) ->
        project.tasks.named("compile${testVariantSlug}JavaWithJavac")
      project.plugins.hasPlugin(KotlinBasePluginWrapper::class.java) ->
        project.tasks.named("compile${testVariantSlug}Kotlin")
      else -> error("Currently, only supporting Java or Kotlin projects.")
    }.configure {
      it.dependsOn(writeResourcesTask)
    }

    val testTaskProvider = configureTestTask(project, testVariantSlug, writeResourcesTask, reportOutputFolder)
    recordTaskProvider.configure {
      it.dependsOn(testTaskProvider)
    }

    verifyTaskProvider.configure {
      it.dependsOn(testTaskProvider)
    }
  }

  private fun getPaparazziMode(project: Project) = when {
    project.hasProperty("PAPARAZZI_VERIFY") -> "verify"
    project.hasProperty("PAPARAZZI_RECORD") -> "record"
    else -> "ignore"
  }
}