/*
 * Copyright 2015 Netflix, Inc.
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

package com.netflix.spinnaker.gradle.project

import com.netflix.spinnaker.gradle.baseproject.SpinnakerBaseProjectPlugin
import com.netflix.spinnaker.gradle.publishing.artifactregistry.ArtifactRegistryPublishPlugin
import com.netflix.spinnaker.gradle.publishing.PublishingPlugin
import com.netflix.spinnaker.gradle.publishing.nexus.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.AbstractCopyTask

class SpinnakerProjectPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.plugins.apply(SpinnakerBaseProjectPlugin)
    project.plugins.apply(PublishingPlugin)
    project.plugins.apply(ArtifactRegistryPublishPlugin)
    project.plugins.apply(NexusPublishPlugin)
    project.tasks.withType(AbstractCopyTask) {
       it.configure {setProperty("duplicatesStrategy",DuplicatesStrategy.EXCLUDE)}
    }
  }
}
