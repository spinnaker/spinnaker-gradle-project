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

plugins {
  // Apply the Kotlin JVM plugin to add support for Kotlin.
  id("org.jetbrains.kotlin.jvm").version("1.3.72")
  `kotlin-dsl`
}

dependencies {
  implementation("org.gradle.crypto.checksum:org.gradle.crypto.checksum.gradle.plugin:1.2.0")

  implementation(platform("com.fasterxml.jackson:jackson-bom:2.11.1"))
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")

  // Kotlin standard library.
  implementation("org.jetbrains.kotlin:kotlin-stdlib")

  // Kotlin test library.
  testImplementation("org.jetbrains.kotlin:kotlin-test")

  // Kotlin JUnit integration.
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

gradlePlugin {
  // Define the plugin
  plugins {
    create("serviceExtension") {
      id = "io.spinnaker.plugin.service-extension"
      implementationClass = "com.netflix.spinnaker.gradle.extension.SpinnakerServiceExtensionPlugin"
    }

    create("uiExtension") {
      id = "io.spinnaker.plugin.ui-extension"
      implementationClass = "com.netflix.spinnaker.gradle.extension.SpinnakerUIExtensionPlugin"
    }

    create("bundler") {
      id = "io.spinnaker.plugin.bundler"
      implementationClass = "com.netflix.spinnaker.gradle.extension.SpinnakerExtensionsBundlerPlugin"
    }

    create("compatibilityTestRunner") {
      id = "io.spinnaker.plugin.compatibility-test-runner"
      implementationClass = "com.netflix.spinnaker.gradle.extension.compatibility.SpinnakerCompatibilityTestRunnerPlugin"
    }
  }
}

pluginBundle {
  website = "https://spinnaker.io"
  vcsUrl = "https://github.com/spinnaker/spinnaker-gradle-project"
  description = "Spinnaker extension development plugins"
  tags = listOf("spinnaker")

  (plugins) {
    "serviceExtension" {
      displayName = "Spinnaker service extension development plugin"
    }

    "uiExtension" {
      displayName = "Spinnaker UI extension development plugin"
    }

    "bundler" {
      displayName = "Spinnaker extension bundler plugin"
    }

    "compatibilityTestRunner" {
      displayName = "Spinnaker compatibility test runner"
    }
  }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionaltest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionaltestImplementation").extendsFrom(configurations.getByName("testImplementation"))

// Add a task to run the functional tests
val functionalTest by tasks.creating(Test::class) {
  testClassesDirs = functionalTestSourceSet.output.classesDirs
  classpath = functionalTestSourceSet.runtimeClasspath
}

val check by tasks.getting(Task::class) {
  // Run the functional tests as part of `check`
  dependsOn(functionalTest)
}

