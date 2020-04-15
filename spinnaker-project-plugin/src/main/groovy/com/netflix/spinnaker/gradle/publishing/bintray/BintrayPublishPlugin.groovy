package com.netflix.spinnaker.gradle.publishing.bintray

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.spinnaker.gradle.Flags
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

class BintrayPublishPlugin implements Plugin<Project> {
  @Override
  void apply(Project project) {
    project.plugins.withType(MavenPublishPlugin) {
      project.extensions.create("bintraySpinnaker", BintrayPublishExtension, project)
      if (Flags.shouldAddPublishingOpinions(project)) {
        project.afterEvaluate {
          def extension = project.extensions.getByType(BintrayPublishExtension)
          if (extension.shouldPublishJar()) {
            project.extensions.configure(PublishingExtension) { publishing ->
              publishing.repositories.maven { MavenArtifactRepository repo ->
                repo.name = 'bintray'
                repo.url = extension.getJarPublishUri()
                repo.credentials {
                  username = extension.getBintrayUser()
                  password = extension.getBintrayKey()
                }
              }
            }
          }
          project.tasks.withType(Deb) { Deb deb ->
            if (extension.shouldPublishDeb()) {
              String taskName = "publish${deb.name.charAt(0).toUpperCase()}${deb.name.substring(1)}"
              def publishDeb = project.tasks.create(taskName, BintrayDebUploadTask, deb)
              project.tasks.getByName('publish').dependsOn(publishDeb)
            }
          }
        }
      }
    }
  }
}
