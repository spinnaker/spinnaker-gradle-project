package com.netflix.spinnaker.gradle

import org.gradle.api.Project

class Flags {

  /**
   * Whether or not the {@code targetJava17} property was set.
   */
  static boolean targetJava17(Project project) {
    return Boolean.valueOf(project.findProperty("targetJava17")?.toString())
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
