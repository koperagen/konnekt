## Description

Konnekt - attempt to reimplement Retrofit as a compiler plugin. Playground for investigating meta-programming & tooling integration capabilities of Kotlin compiler & Arrow Meta.

## Usage

### Build

Checkout `mpp` branch and run `publishToMavenLocal` task.
For CLI gradle it would be `./gradlew publishToMavenLocal`

### Use
Once dependency available in maven local, project setup should be as follows

*build.gradle*
```kotlin
import konnekt.gradle.Konnekt

plugins {
    kotlin("jvm") version "1.4.10"
    id("io.github.koperagen.konnekt") version "1.4.10-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.github.koperagen:prelude-jvm:1.4.10-SNAPSHOT")
  
    // Or any other ktor module required to customize client
    implementation("io.ktor:ktor-client-apache:${Konnekt.ktorVersion}")
}
```

*settings.gradle*
```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

### Examples
Configured Gradle projects with some sample code available at [Playground repository](https://github.com/koperagen/konnekt_playground)

## Roadmap

- [x] Define, refine the HTTP request model
- [x] Parse request model from DSL
- [x] Provide compiler messages about erroneous models
- [x] Diagnostics and quick fixes to restrict DSL usages
- [x] Generate sources for limited subset of annotations (basic GET, DELETE, POST etc)
- [x] Gradle plugin to apply compiler plugin to project
- [x] Implement FormUrlEncoded & Multipart
- [ ] Configure Gradle projects to resolve generated code in IDEA via Gradle plugin
- [ ] Resolve annotation & types via Typed Quotes
- [ ] Add & test android build variants
- [ ] Samples & docs
- [ ] Investigate IR capabilities
- [ ] Prototype compile-time verified converters

## Current limitations
1. JVM only
> Codegen in Konnekt is implemented using Quote System of Arrow Meta. It only supports compilation to JVM target, but this limitation will be lifted sometime after Kotlin 1.5 release
