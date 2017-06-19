package com.netflix.spinnaker.gradle.ospackage

import com.netflix.gradle.plugins.packaging.ProjectPackagingExtension
import nebula.plugin.ospackage.application.OspackageApplicationPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ApplicationPluginConvention
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.application.CreateStartScripts
import org.gradle.api.tasks.bundling.Jar
import org.redline_rpm.header.Os
import org.redline_rpm.payload.Directive

class SpinnakerPackagePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        String appName = project.rootProject.name
        project.plugins.apply(OspackageApplicationPlugin)
        def appConvention = project.convention.getPlugin(ApplicationPluginConvention)
        appConvention.applicationName = appName
        appConvention.applicationDistribution.from(project.file("config/${appName}.yml")) {
            into('config')
        }
        appConvention.applicationDefaultJvmArgs << "-Djava.security.egd=file:/dev/./urandom"

        project.tasks.withType(CreateStartScripts) {
            it.defaultJvmOpts  = appConvention.applicationDefaultJvmArgs + ["-Dspring.config.location=/opt/spinnaker/config/"]
            it.doLast {
                unixScript.text = unixScript.text.replace('DEFAULT_JVM_OPTS=', '''\
                    if [ -f /etc/default/spinnaker ]; then
                      set -a
                      . /etc/default/spinnaker
                      set +a
                    fi
                    DEFAULT_JVM_OPTS='''.stripIndent())
                unixScript.text = unixScript.text.replace('CLASSPATH=$APP_HOME', 'CLASSPATH=$APP_HOME/config:$APP_HOME')
                windowsScript.text = windowsScript.text.replace('set CLASSPATH=', 'set CLASSPATH=%APP_HOME%\\config;')
            }
        }

        project.plugins.withType(JavaBasePlugin)
        def java = project.convention.getPlugin(JavaPluginConvention)
        def mainSrc = java.sourceSets.getByName('main')
        mainSrc.resources.srcDir('src/main/resources')
        mainSrc.resources.srcDir('config')

        project.tasks.withType(Jar) {
            it.exclude("${appName}.yml")
        }

        ProjectPackagingExtension extension = project.extensions.getByType(ProjectPackagingExtension)
        extension.setOs(Os.LINUX)
        String projVer = project.version.toString()
        int idx = projVer.indexOf('-')
        if (idx != -1) {
            extension.setVersion(projVer.substring(0, idx))
        } else {
            extension.setVersion(projVer)
        }

        extension.setPackageName('spinnaker-' + appName)
        def postInstall = project.file('pkg_scripts/postInstall.sh')
        if (postInstall.exists()) {
            extension.postInstall(postInstall)
        }
        def postUninstall = project.file('pkg_scripts/postUninstall.sh')
        if (postUninstall.exists()) {
            extension.postUninstall(postUninstall)
        }

        def upstartConf = project.file("etc/init/${appName}.conf")
        if (upstartConf.exists()) {
            extension.from(upstartConf) {
                into('/etc/init')
                setUser('root')
                setPermissionGroup('root')
                setFileType(new Directive(Directive.RPMFILE_CONFIG | Directive.RPMFILE_NOREPLACE))
            }
        }

        /* Determine the distro and set the path for systemd unit files
           if apt exits 0 and yum exits 1 then this is a debian-based
           OS. Conversely, if apt exits 1 and yum exits 0 it is a
           redhat-based OS.
        */
        String systemdPath
        def apt = 'which apt'.execute()
        def yum = 'which yum'.execute()
        apt.waitFor()
        yum.waitFor()
        if (apt.exitValue() == 0) && (yum.exitValue() == 1) {
            systemdPath = '/lib/systemd/system'
        } else if (apt.exitValue() == 1) && (yum.exitValue() == 0) {
            systemdPath = '/usr/lib/systemd/system'
        }

        def systemdService = project.file("lib/systemd/system/${appName}.service")
        if (systemdService.exists()) {
            extension.from(systemdService) {
                into(systemdPath)
                setUser('root')
                setPermissionGroup('root')
                setFileType(new Directive(Directive.RPMFILE_CONFIG | Directive.RPMFILE_NOREPLACE))
            }
        }

        def logrotateConf = project.file("etc/logrotate.d/${appName}")
        if (logrotateConf.exists()) {
            extension.from(logrotateConf) {
                into('/etc/logrotate.d')
                setUser('root')
                setPermissionGroup('root')
                setFileMode(0644)
                setFileType(new Directive(Directive.RPMFILE_CONFIG | Directive.RPMFILE_NOREPLACE))
            }
        }
    }
}
