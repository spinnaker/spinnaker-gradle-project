/*
 * Copyright 2019 Netflix, Inc.
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

package com.netflix.spinnaker.gradle.extension

import com.netflix.spinnaker.gradle.extension.Plugins.ASSEMBLE_PLUGIN_TASK_NAME
import com.netflix.spinnaker.gradle.extension.extensions.SpinnakerBundleExtension
import com.netflix.spinnaker.gradle.extension.extensions.SpinnakerPluginExtension
import com.netflix.spinnaker.gradle.extension.tasks.AssembleJavaPluginZipTask
import groovy.json.JsonOutput
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import java.io.File
import java.lang.IllegalStateException

/**
 * Gradle plugin to support spinnaker service plugin bundling aspects.
 *
 * TODO(rz): Add spinnaker bintray to repositories
 * TODO(rz): Configure plugin manifest
 * TODO(rz): Auto-add `annotationProcessor "org.pf4j:pf4j:3.2.0"`
 */
class SpinnakerServiceExtensionPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    project.plugins.apply(JavaPlugin::class.java)
    project.extensions.create("spinnakerPlugin", SpinnakerPluginExtension::class.java)
    project.tasks.register(ASSEMBLE_PLUGIN_TASK_NAME, AssembleJavaPluginZipTask::class.java)

    project.tasks.getByName("jar")
      .doFirst {
        (it as Jar).createPluginManifest(project)
      }
  }

  private fun Jar.createPluginManifest(project: Project) {
    val pluginExt = project.extensions.findByType(SpinnakerPluginExtension::class.java)
      ?: throw IllegalStateException("A 'spinnakerPlugin' configuration block is required")

    val bundleExt = project.rootProject.extensions.findByType(SpinnakerBundleExtension::class.java)
      ?: throw IllegalStateException("A 'spinnakerBundle' configuration block is required")

    val attributes = mutableMapOf<String, String>()

    applyAttributeIfSet(attributes, "Plugin-Class", pluginExt.pluginClass)
    applyAttributeIfSet(attributes, "Plugin-Id", bundleExt.pluginId)
    applyAttributeIfSet(attributes, "Plugin-Version", bundleExt.version)
    applyAttributeIfSet(attributes, "Plugin-Dependencies", pluginExt.dependencies)
    applyAttributeIfSet(attributes, "Plugin-Requires", pluginExt.requires)
    applyAttributeIfSet(attributes, "Plugin-Description", bundleExt.description)
    applyAttributeIfSet(attributes, "Plugin-Provider", bundleExt.provider)
    applyAttributeIfSet(attributes, "Plugin-License", bundleExt.license)

    manifest.attributes(attributes)

    //TODO: Generation of the plugin ref can be conditional and also make the location of the file configurable.
    createPluginRef(project, "${bundleExt.pluginId}-${pluginExt.serviceName}", this.temporaryDir.absolutePath)

  }

  private fun createPluginRef(project: Project, pluginExtensionName: String?, manifestLocation: String) {
    val sourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets

    val classesDirs: List<String>  = sourceSets.getByName("main").runtimeClasspath.files.map { it ->
      it.absolutePath
    }
    val libDirs: List<String>  = sourceSets.getByName("main").runtimeClasspath.files
      .filter { it.absolutePath.endsWith(".jar") }
      .map { it.parent }
    val pluginRelInfo = mapOf(
      "pluginPath" to manifestLocation,
      "classesDirs" to classesDirs,
      "libsDirs" to listOf("${project.buildDir}/lib", *(libDirs.toTypedArray()))
    )

    File(project.buildDir, "${pluginExtensionName ?: project.name}.plugin-ref").writeText(
      JsonOutput.prettyPrint(JsonOutput.toJson(pluginRelInfo))
    )
  }

  private fun applyAttributeIfSet(attributes: MutableMap<String, String>, key: String, value: String?) {
    if (value != null) {
      attributes[key] = value
    }
  }
}
