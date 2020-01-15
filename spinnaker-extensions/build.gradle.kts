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
    id("org.jetbrains.kotlin.jvm").version("1.3.31")
}


dependencies {
    // Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

gradlePlugin {
    // Define the plugin
    val spinnakerserviceextension by plugins.creating {
        id = "com.netflix.spinnaker.gradle.extension.spinnakerserviceextension"
        implementationClass = "com.netflix.spinnaker.gradle.extension.SpinnakerServiceExtensionPlugin"
    }

    val spinnakeruiextension by plugins.creating {
        id = "com.netflix.spinnaker.gradle.extension.spinnakeruiextension"
        implementationClass = "com.netflix.spinnaker.gradle.extension.SpinnakerUIExtensionPlugin"
    }

    val spinnakerextensionsbundler by plugins.creating {
        id = "com.netflix.spinnaker.gradle.extension.spinnakerextensionsbundler"
        implementationClass = "com.netflix.spinnaker.gradle.extension.SpinnakerExtensionsBundlerPlugin"
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)
configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))

// Add a task to run the functional tests
val functionalTest by tasks.creating(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

val check by tasks.getting(Task::class) {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

