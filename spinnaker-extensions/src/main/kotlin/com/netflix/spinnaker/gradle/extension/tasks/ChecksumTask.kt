/*
 * Copyright 2019 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.netflix.spinnaker.gradle.extension.tasks

import org.gradle.api.AntBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task to calculate checksum for each plugin artifact
 */
open class ChecksumTask : DefaultTask() {

   init {
       outputs.upToDateWhen { false } // No caching.
   }

    @TaskAction
    fun doAction() {

        if (project.subprojects.size > 0) {
            project.subprojects.forEach { project: Project ->
                File(project.buildDir.absolutePath + "/libs").walk().forEach { file: File ->
                    if (file.isFile) {
                        project.logger.debug("File: {}", file.toString())
                        project.ant { ant: AntBuilder ->
                            ant.invokeMethod("checksum", mapOf("file" to file))
                        }
                    }
                }
            }
            return
        }
        project.logger.log(LogLevel.WARN, "Nothing to checksum!!")
    }

}
