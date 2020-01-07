package com.netflix.spinnaker.gradle.pf4j

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Unit test for the 'com.netflix.spinnaker.gradle.pf4j.spinnakerpf4j' plugin.
 */
class SpinnakerPf4jGradlePluginTest {
    @Test fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("com.netflix.spinnaker.gradle.pf4j.spinnakerpf4j")

        // Verify the result
        assertNotNull(project.tasks.findByName("computeChecksum"))
        assertNotNull(project.tasks.findByName("registerPlugin"))
        assertNotNull(project.tasks.findByName("distPluginZip"))
    }
}
