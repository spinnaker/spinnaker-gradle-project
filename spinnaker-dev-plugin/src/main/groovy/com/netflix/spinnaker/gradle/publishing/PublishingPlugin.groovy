package com.netflix.spinnaker.gradle.publishing

import com.netflix.spinnaker.gradle.Flags
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlatformPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar

class PublishingPlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    project.plugins.withType(JavaLibraryPlugin) {
      project.plugins.apply(MavenPublishPlugin)
      if (Flags.shouldAddPublishingOpinions(project)) {
        project.logger.info "adding maven publication for java library in $project.name"
        project.extensions.configure(PublishingExtension) { publishingExtension ->
          publishingExtension.publications.create("spinnaker", MavenPublication) { pub ->
            pub.from(project.components.getByName("java"))
            project.tasks.withType(Jar) { Jar jar ->
              if (jar.name == 'sourceJar') {
                pub.artifact(jar)
              }
            }
          }
        }
      }
    }

    project.plugins.withType(JavaPlatformPlugin) {
      project.plugins.apply(MavenPublishPlugin)
      if (Flags.shouldAddPublishingOpinions(project)) {
        project.logger.info "adding maven publication for java platform in $project.name"
        project.extensions.configure(PublishingExtension) { publishingExtension ->
          publishingExtension.publications.create("spinnaker", MavenPublication) { pub ->
            pub.from(project.components.getByName("javaPlatform"))
          }
        }
      }
    }
  }
}
