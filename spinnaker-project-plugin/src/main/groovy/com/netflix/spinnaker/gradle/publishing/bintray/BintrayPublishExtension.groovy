package com.netflix.spinnaker.gradle.publishing.bintray

import com.netflix.gradle.plugins.deb.Deb
import org.gradle.api.Project

class BintrayPublishExtension {

  BintrayPublishExtension(Project project) {
    this.project = project
    if (project.findProperty("bintrayOrg")) {
      this.bintrayOrg = project.property("bintrayOrg")
    }
    if (project.findProperty("bintrayPublishEnabled")) {
      this.enabled = Boolean.valueOf(project.property("bintrayPublishEnabled").toString())
    }
    if (project.findProperty("bintrayPublishJarEnabled")) {
      this.jarEnabled = Boolean.valueOf(project.property("bintrayJarPublish").toString())
    }
  }

  Project project

  boolean enabled = true

  Boolean jarEnabled
  Boolean debEnabled

  String bintrayOrg = 'cfieber' //spinnaker
  String bintrayJarRepo = 'buildtestrepo' //spinnaker
  String bintrayJarPackage = null

  String bintrayDebRepo = 'debtestrepo' //debians

  String debDistribution = 'trusty'
  String debComponent = 'spinnaker'
  String debArchitectures = 'i386,amd64'
  String debBuildNumber = ''
  Integer publishWaitForSecs


  private <T> T prop(Class<T> type, String prop, T defaultValue) {
    Object value = project.findProperty(prop)
    if (value == null) {
      return defaultValue
    }
    return value.asType(type)
  }

  boolean getEnabled() {
    return prop(Boolean, "bintrayPublishEnabled", enabled)
  }

  boolean getJarEnabled() {
    return getEnabled() && prop(Boolean, "bintrayPublishJarEnabled", jarEnabled != null ? jarEnabled : true)
  }

  boolean getDebEnabled() {
    return getEnabled() && prop(Boolean, "bintrayPublishDebEnabled", debEnabled != null ? debEnabled : true)
  }

  String getBintrayOrg() {
    return prop(String, "bintrayPublishOrg", bintrayOrg ?: getBintrayUser())
  }

  String getBintrayJarRepo() {
    return prop(String, "bintrayJarRepo", bintrayJarRepo)
  }

  String getBintrayJarPackage() {
    return prop(String, "bintrayJarPackage", bintrayJarPackage == null ? project.rootProject.name : bintrayJarPackage)
  }

  String getBintrayUser() {
    return prop(String, "bintrayUser", null)
  }

  String getBintrayKey() {
    return prop(String, "bintrayKey", null)
  }

  String getBasicAuthHeader() {
    "Basic " + "${getBintrayUser()}:${getBintrayKey()}".getBytes('UTF-8').encodeBase64()
  }

  String getBintrayDebRepo() {
    return prop(String, "bintrayPackageRepo", bintrayDebRepo)
  }

  String getDebDistribution() {
    return prop(String, "bintrayPackageDebDistribution", debDistribution)
  }

  String getDebComponent() {
    return debComponent
  }

  String getDebArchitectures() {
    return debArchitectures
  }

  String getDebBuildNumber() {
    return prop(String, "bintrayPackageBuildNumber", debBuildNumber)
  }

  Integer getPublishWaitForSecs() {
    return prop(Integer, "bintrayPublishWaitForSecs", publishWaitForSecs == null ? 0 : publishWaitForSecs)
  }

  boolean hasCreds() {
    getBintrayUser() && getBintrayKey()
  }

  boolean shouldPublishJar() {
    return shouldPublish("jar", this.&getJarEnabled)
  }

  String getJarPublishUri() {
    return "https://api.bintray.com/maven/${getBintrayOrg()}/${getBintrayJarRepo()}/${getBintrayJarPackage()}/;publish=1"
  }

  String getDebPublishUri(Deb deb) {
    def packageName = deb.packageName
    def poolPath = "pool/main/${packageName.charAt(0)}/$packageName"
    def debFileName = deb.archiveFile.get().getAsFile().name
    String versionName = project.version.toString()
    if (versionName.endsWith('-SNAPSHOT')) {
      versionName = versionName.replaceAll(/SNAPSHOT/, Long.toString(System.currentTimeMillis()))
    }

    return "https://api.bintray.com/content/${getBintrayOrg()}/${getBintrayDebRepo()}/$packageName/$versionName/$poolPath/$debFileName;deb_distribution=${getDebDistribution()};deb_component=${getDebComponent()};deb_architecture=${getDebArchitectures()};publish=1"
  }

  private boolean shouldPublish(String type, Closure<Boolean> enabledCheck) {
    boolean publish = true
    if (!enabledCheck()) {
      publish = false
      project.logger.info("not publishing $type for project $project.name, bintrayPublishEnabled: ${getEnabled()}, ${type}Enabled: ${enabledCheck()}")
    }

    if (!hasCreds()) {
      publish = false
      project.logger.info("not publishing $type for project $project.name, ensure bintrayUser and bintrayKey properties are set")
    }

    return publish
  }

  boolean shouldPublishDeb() {
    return shouldPublish("deb", this.&getDebEnabled)
  }
}
