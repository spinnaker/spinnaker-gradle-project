/*
 * Copyright 2018 Netflix, Inc.
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
package com.netflix.spinnaker.gradle.codestyle

import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.JavaExtension
import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

class SpinnakerCodeStylePlugin implements Plugin<Project> {

  @Override
  void apply(Project project) {
    def extension = project.extensions.create("spinnakerCodeStyle", SpinnakerCodeStyle)

    project.afterEvaluate {
      if (!extension.enabled) {
        project.logger.warn("${project.name} has disabled codestyle enforcement!")
        return
      }

      project.rootProject.file(".git/hooks").mkdirs()
      project.rootProject.file(".git/hooks/pre-commit").write(getClass().getResource("/pre-commit").text)
      project.rootProject.file(".git/hooks/pre-commit").executable = true

      project.plugins.apply(SpotlessPlugin)
      project.plugins.withType(SpotlessPlugin) { SpotlessPlugin spotless ->
        spotless.extension.java(new Action<JavaExtension>() {
          @Override
          void execute(JavaExtension javaExtension) {
            javaExtension.target("src/**/*.java")
            javaExtension.googleJavaFormat()
            javaExtension.removeUnusedImports()
            javaExtension.trimTrailingWhitespace()
            javaExtension.endWithNewline()
          }
        })

        if (hasKotlin(project)) {
          spotless.extension.kotlin(new Action<KotlinExtension>() {
            @Override
            void execute(KotlinExtension kotlinExtension) {
              kotlinExtension.ktlint("0.31.0").userData([
                indent_size: '2',
                continuation_indent_size: '2'
              ])
              kotlinExtension.trimTrailingWhitespace()
              kotlinExtension.endWithNewline()
            }
          })
        }

        spotless.extension.format(
          'misc',
          new Action<FormatExtension>() {
            @Override
            void execute(FormatExtension formatExtension) {
              formatExtension.target('**/.gitignore', 'src/**/*.json', 'src/**/*.yml', 'src/**/*.yaml', 'config/*.yml', 'halconfig/*.yml', '**/*.gradle')
              formatExtension.trimTrailingWhitespace()
              formatExtension.indentWithSpaces(2)
              formatExtension.endWithNewline()
            }
          }
        )
      }
    }
  }

  private boolean hasKotlin(Project project) {
    Class kotlin
    try {
      kotlin = Class.forName("org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin")
    } catch (ClassNotFoundException e) {
      return false
    }
    return !project.plugins.withType(kotlin).empty
  }
}
