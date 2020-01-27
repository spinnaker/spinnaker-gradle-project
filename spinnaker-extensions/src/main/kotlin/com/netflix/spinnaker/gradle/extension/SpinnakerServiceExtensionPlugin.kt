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
import com.netflix.spinnaker.gradle.extension.tasks.RegistrationTask
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import java.io.File
import java.lang.IllegalStateException
import java.net.URL

/**
 * Gradle plugin to support spinnaker service plugin bundling aspects.
 */
class SpinnakerServiceExtensionPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    if (!project.plugins.hasPlugin(JavaPlugin::class.java)) {
      project.plugins.apply(JavaPlugin::class.java)
    }

    project.extensions.create("spinnakerPlugin", SpinnakerPluginExtension::class.java)
    project.tasks.register(ASSEMBLE_PLUGIN_TASK_NAME, AssembleJavaPluginZipTask::class.java)

    project.tasks.getByName("jar")
      .doFirst {
        (it as Jar).createPluginManifest(project)
      }

    // Add the Spinnaker bintray repository
    project.repositories.add(project.repositories.maven {
      it.url = URL("https://dl.bintray.com/spinnaker/spinnaker/").toURI()
    })

    // Add the PF4J annotation processor to the dependencies
    project.gradle.addListener(object : DependencyResolutionListener {
      override fun beforeResolve(dependencies: ResolvableDependencies) {
        val compileDeps = project.configurations.getByName("annotationProcessor").dependencies
        compileDeps.add(project.dependencies.create("org.pf4j:pf4j:${Plugins.PF4J_VERSION}"))
      }

      override fun afterResolve(dependencies: ResolvableDependencies) {}
    })
  }

  private fun Jar.createPluginManifest(project: Project) {
    val pluginExt = project.extensions.findByType(SpinnakerPluginExtension::class.java)
      ?: throw IllegalStateException("A 'spinnakerPlugin' configuration block is required")

    val bundleExt = project.rootProject.extensions.findByType(SpinnakerBundleExtension::class.java)
      ?: throw IllegalStateException("A 'spinnakerBundle' configuration block is required")

    val attributes = mutableMapOf<String, String>()

    applyAttributeIfSet(attributes, "Plugin-Class", pluginExt.pluginClass)
    applyAttributeIfSet(attributes, "Plugin-Id", "${bundleExt.pluginId}-${pluginExt.serviceName}")
    applyAttributeIfSet(attributes, "Plugin-Version", bundleExt.version)
    applyAttributeIfSet(attributes, "Plugin-Dependencies", pluginExt.dependencies)
    applyAttributeIfSet(attributes, "Plugin-Description", bundleExt.description)
    applyAttributeIfSet(attributes, "Plugin-Provider", bundleExt.provider)
    applyAttributeIfSet(attributes, "Plugin-License", bundleExt.license)

    manifest.attributes(attributes)
  }

  private fun applyAttributeIfSet(attributes: MutableMap<String, String>, key: String, value: String?) {
    if (value != null) {
      attributes[key] = value
    }
  }
}
