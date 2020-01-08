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

import com.netflix.spinnaker.gradle.extension.tasks.ChecksumTask
import com.netflix.spinnaker.gradle.extension.tasks.RegistrationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

/**
 * Gradle plugin to support spinnaker plugin development life cycle.
 */
class SpinnakerExtensionPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        project.tasks.register("computeChecksum", ChecksumTask::class.java)
        project.tasks.register("registerPlugin", RegistrationTask::class.java)

        // Register assemble service plugin task for each sub project.
        val allBuildDirs: MutableList<String> = mutableListOf()
        project.subprojects.forEach { subProject ->
            allBuildDirs.add("${subProject.name}/build/libs")

            if (subProject.plugins.hasPlugin(JavaPlugin::class.java)) {
                subProject.logger.warn("Adding assemble for ${subProject.name}......")
                val jar = subProject.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
                val childSpec: CopySpec = subProject.copySpec().with(jar).into("classes")
                val libSpec: CopySpec = subProject.copySpec().from(subProject.configurations.getByName("runtimeClasspath")).into("lib")
                subProject.tasks.register<Jar>("assembleServicePluginZip", Jar::class.java) {
                    it.archiveBaseName.set(subProject.name)
                    it.archiveExtension.set("zip")
                    it.with(childSpec, libSpec)
                    it.dependsOn(subProject.tasks.findByName("jar"))
                }
            } else {
                subProject.tasks.register("assembleServicePluginZip") {
                    subProject.logger.quiet("Not a java lib. This task does nothing!")
                }
            }
        }
        project.logger.debug(allBuildDirs.toString())

        // Register distPluginZip for root project.
        project.tasks.register<Zip>("distPluginZip", Zip::class.java) {
            it.from(allBuildDirs).into("/")
            it.archiveFileName.set("${project.name}-${project.version}.zip")
            it.include("*")
            it.destinationDirectory.set(project.rootDir)
        }

        val computeChecksumTask: Task = project.tasks.getByName("computeChecksum")
        project.afterEvaluate {
            project.subprojects.forEach { subProject ->
                subProject.afterEvaluate { pluginProject ->
                    pluginProject.tasks.getByName("build").finalizedBy(pluginProject.tasks.getByName("assembleServicePluginZip"))
                    computeChecksumTask.dependsOn.add(pluginProject.tasks.getByName("build"))
                }
            }
            project.tasks.getByName("distPluginZip").dependsOn(computeChecksumTask)
        }
    }
}


