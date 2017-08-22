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

package com.netflix.spinnaker.gradle.idea

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.yaml.snakeyaml.Yaml

class SpinnakerIdeaConfigPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(IdeaPlugin)
        project.plugins.withType(IdeaPlugin) { IdeaPlugin idea ->
            if (project.rootProject == project) {
                project.plugins.withType(JavaPlugin) {
                    JavaPluginConvention convention = project.convention.getPlugin(JavaPluginConvention)
                    idea.model.project.jdkName = convention.sourceCompatibility
                    idea.model.project.languageLevel = convention.targetCompatibility
                }
                idea.model.project.vcs = 'Git'
                idea.model.project.ipr.withXml { XmlProvider xp ->
                    def projectNode = xp.asNode()
                    (projectNode.component.find { it.@name == 'GradleSettings' } ?:
                        projectNode.appendNode("component", [name: 'GradleSettings'])).replaceNode {
                        component(name: 'GradleSettings') {
                            option(name: 'linkedExternalProjectsSettings') {
                                GradleProjectSettings() {
                                    option(name: 'distributionType', value: 'DEFAULT_WRAPPED')
                                    option(name: 'externalProjectPath', value: '$PROJECT_DIR$')
                                    option(name: 'useAutoImport', value: 'true')
                                }
                            }
                        }
                    }

                    (projectNode.component.find { it.@name == 'CopyrightManager' } ?:
                            projectNode.appendNode("component", [name: 'CopyrightManager'])).replaceNode {
                        component(name: 'CopyrightManager', 'default': 'ASL2') {
                            copyright() {
                                option(name: 'notice', value: COPYRIGHT_TEXT)
                                option(name: 'keyword', value: 'Copyright')
                                option(name: 'allowReplaceKeyword', value: '')
                                option(name: 'myName', value: 'ASL2')
                                option(name: 'myLocal', value: 'true')
                            }
                        }
                    }

                    File intellijRunConfigs = new File(System.getProperty('user.home') + '/.spinnaker/intellij.yml')
                    if (intellijRunConfigs.canRead()) {
                        Map<String, Object> manifest = new Yaml().load(intellijRunConfigs.text)
                        if (manifest.containsKey(project.name)) {
                            (projectNode.component.find { it.@name == 'ProjectRunConfigurationManager' } ?:
                                    projectNode.appendNode('component', [name: 'ProjectRunConfigurationManager'])).replaceNode {

                                manifest[project.name]?.each { configName, runConfig ->
                                    component(name: 'ProjectRunConfigurationManager') {
                                        configuration('default': 'false', name: configName, type: 'Application', factoryName: 'Application') {
                                            extension(name: 'coverage', enabled: 'false', merge: 'false', runner: 'idea')
                                            option(name: 'MAIN_CLASS_NAME', value: runConfig.mainClassName)
                                            option(name: 'VM_PARAMETERS', value: runConfig.vmParameters?.replaceAll('\n', ' '))
                                            option(name: 'PROGRAM_PARAMETERS', value: '')
                                            option(name: 'WORKING_DIRECTORY', value: 'file://$PROJECT_DIR$')
                                            option(name: 'ALTERNATIVE_JRE_PATH_ENABLED', value: 'false')
                                            option(name: 'ALTERNATIVE_JRE_PATH')
                                            option(name: 'ENABLE_SWING_INSPECTOR', 'false')
                                            option(name: 'ENV_VARIABLES')
                                            option(name: 'PASS_PARENT_ENVS', value: 'true')
                                            module(name: runConfig.moduleName)
                                            envs()
                                            method()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static final String COPYRIGHT_TEXT =
            'Copyright $today.year Netflix, Inc.\n\n' +
            'Licensed under the Apache License, Version 2.0 (the "License")\n' +
            'you may not use this file except in compliance with the License.\n' +
            'You may obtain a copy of the License at\n\n' +
            '  http://www.apache.org/licenses/LICENSE-2.0\n\n' +
            'Unless required by applicable law or agreed to in writing, software\n' +
            'distributed under the License is distributed on an "AS IS" BASIS,\n' +
            'WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n' +
            'See the License for the specific language governing permissions and\n' +
            'limitations under the License.'
}
