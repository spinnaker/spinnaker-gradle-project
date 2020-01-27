# Gradle plugin for supporting spinnaker plugin implementations.

This **gradle** plugin allows Spinnaker developers to bundle, publish and register **_"spinnaker plugins"_** with spinnaker.

Plugins are bundled into two artifacts: A "plugin bundle" and a "plugin info" manifest. These can be found in the project's
root build directory: `build/distributions`. The plugin bundle will be a `{your-plugin}-{version}.zip` file, whereas the
plugin info manifest will always be `plugin-info.json`.

An example repository can be found at [robzienert/spinnaker-plugin-helloworld](https://github.com/robzienert/spinnaker-plugin-helloworld).

## Usage

Spinnaker plugin development is broken up into 3 different Gradle plugins within `spinnaker-extensions`:

* `bundler`, applied to the root project only, configures metadata and packages plugins into a bundle consumable by Spinnaker.
* `ui-extension`, applied only to the subproject containing Deck code, configures the module for building Deck plugins.
* `service-extension`, applied only to backend service subprojects, configures the module for building backend service plugins.

```groovy
// build.gradle
buildscript {
  repositories {
    maven { url "https://dl.bintray.com/spinnaker/gradle/" }
  }
  dependencies {
    classpath("com.netflix.spinnaker.gradle:spinnaker-extensions:$spinnakerGradleVersion")
  }
}

// settings.gradle
spinnakerGradleVersion=LATEST_VERSION_HERE
```

* Root project must `apply plugin: "io.spinnaker.plugin.bundler`
* Deck extension module must `apply plugin: "io.spinnaker.plugin.ui-extension`
* Backend extension modules must `apply plugin: "io.spinnaker.plugin.service-extension`

Once configured: `./gradlew releaseBundle`

### Root Module

The root module must not be responsible for building any plugin code.
It's purpose is to configure plugin metadata and bundle service plugins from subprojects within the repository into a single artifact.

```groovy
apply plugin: "io.spinnaker.plugin.bundler"

spinnakerBundle {
  pluginId    = "com.netflix.streaming.platform.cde.aws-rds"
  description = "Provides AWS RDS infrastructure management"
  version     = "1.0.0"
  provider    = "https://github.com/Netflix"
}
```

* `pluginId`: The plugin ID for the bundled plugins.
This should be considered the root namespace of all plugins contained within your repository.
Must follow the [`CanonicalPluginId`](https://github.com/spinnaker/kork/blob/master/kork-plugins/src/main/kotlin/com/netflix/spinnaker/kork/plugins/CanonicalPluginId.kt) format.
* `description`: A description of what the plugin does.
* `version`: The version of the plugin. This value will be inferred from the repository git tags on build if left undefined.
* `provider`: The plugin provider (that's you). Using your Github profile (or something equivalent) can be used.

### Service Modules

Modules for extending backend services like Orca, Clouddriver, Fiat.

```groovy
apply plugin: "io.spinnaker.plugin.service-extension"

dependencies {
  compileOnly("com.netflix.spinnaker.kork:kork-plugins-api:$korkVersion")
}

spinnakerPlugin {
  serviceName        = "orca"
  pluginClassName    = "com.netflix.streaming.platform.cde.AwsRdsPlugin"
  systemRequirements = "orca>=7.0.0" // Will default to `serviceName>=0.0.0` if undefined
}
```

### Deck Module

Module for extending Deck.
The `deck-plugins` package provides many of the conventions needed for plugin development within Deck: This just wires its conventions up to the bundler.

```groovy
apply plugin: "io.spinnaker.plugin.ui-extension"
```

### Notes

* Expects multi-project gradle builds where each sub project implements extensions targeting a single spinnaker service.
* Storage for plugin artifacts(bundled zip) can be like S3, GCS, jCenter or artifactory etc. ????

- [ ] Publish bundle to ??
- [ ] How to register it with spinnaker ??
