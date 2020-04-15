package com.netflix.spinnaker.gradle

import org.gradle.api.Project

class Flags {

  /**
   * Whether project publishing opinions should be added.
   *
   * Determined by the project property 'enablePublishing', and
   * disabled by default.
   *
   * Allows for inclusion of projects into a composite build where
   * other publishing opinions or destinations may be present.
   *
   * @param project the project from which to read the property
   * @return whether publishing opinions should be added
   */
  static boolean shouldAddPublishingOpinions(Project project) {
    return Boolean.valueOf(project.findProperty("enablePublishing")?.toString())
  }

  /**
   * Whether cross-compilation should be enabled.
   *
   * Determined by the project property 'enableCrossCompilerPlugin', and
   * disabled by default.
   *
   * @param project the project from which to read the property
   * @return whether cross-compilation should be enabled
   */
  static boolean shouldEnableCrossCompilation(Project project) {
    return Boolean.valueOf(project.findProperty("enableCrossCompilerPlugin")?.toString())
  }

}
