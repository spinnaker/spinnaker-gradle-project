/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.gradle.extension.tasks

import com.netflix.spinnaker.gradle.extension.Plugins
import com.netflix.spinnaker.gradle.extension.Plugins.CHECKSUM_BUNDLE_TASK_NAME
import com.netflix.spinnaker.gradle.extension.extensions.SpinnakerBundleExtension
import com.netflix.spinnaker.gradle.extension.extensions.SpinnakerPluginExtension
import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.IllegalStateException
import java.time.Instant

/**
 * TODO(rz): Need to expose release state to the world.
 */
open class CreatePluginInfoTask : DefaultTask() {

  override fun getGroup(): String? = Plugins.GROUP

  @Internal
  val rootProjectVersion: String = project.rootProject.version.toString()

  @TaskAction
  fun doAction() {
    val allPluginExts = project.rootProject
      .subprojects
      .mapNotNull { it.extensions.findByType(SpinnakerPluginExtension::class.java) }
      .toMutableList()

    val bundleExt = project.rootProject.extensions.findByType(SpinnakerBundleExtension::class.java)
      ?: throw IllegalStateException("A 'spinnakerBundle' configuration block is required")

    val requires = allPluginExts.map { it.requires ?: "${it.serviceName}>=0.0.0" }
      .let {
        if (Plugins.hasDeckPlugin(project)) {
          it + "deck>=0.0.0"
        } else {
          it
        }
      }
      .joinToString(",")

    val pluginInfo = mapOf(
      "id" to bundleExt.pluginId,
      "description" to bundleExt.description,
      "provider" to bundleExt.provider,
      "releases" to listOf(
        mapOf(
          "version" to if (isVersionSpecified(bundleExt.version)) bundleExt.version else rootProjectVersion,
          "date" to Instant.now().toString(),
          "requires" to requires,
          "sha512sum" to getChecksum(),
          "state" to "RELEASE"
        )
      )
    )

    // TODO(rz): Is it bad to put the plugin-info into the distributions build dir?
    File(project.buildDir, "distributions/plugin-info.json").writeText(
      JsonOutput.prettyPrint(JsonOutput.toJson(pluginInfo))
    )
  }

  private fun isVersionSpecified(version: String): Boolean {
    return version.isNotBlank() && version.isNotEmpty() && version != "unspecified"
  }

  private fun getChecksum(): String {
    return project.rootProject.tasks
      .getByName(CHECKSUM_BUNDLE_TASK_NAME)
      .outputs
      .files
      .files
      .first()
      .listFiles()
      .first()
      .readText()
  }
}
