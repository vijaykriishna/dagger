---
layout: default
title: Dagger Versions
---

## Dagger release artifacts {#release-artifacts}

Dagger's release artifacts can be found at
[https://repo1.maven.org/maven2/com/google/dagger/](https://repo1.maven.org/maven2/com/google/dagger/).

The release notes for each version can be found at
[https://github.com/google/dagger/releases](https://github.com/google/dagger/releases).

## Dagger `HEAD-SNAPSHOT` artifacts {#head-snapshot-artifacts}

Dagger's `HEAD-SNAPSHOT` artifacts can be used to try out the latest Dagger
changes at HEAD. Unlike the [Dagger release artifacts](#release-artifacts), the
`HEAD-SNAPSHOT` artifacts are updated on each commit to the Dagger codebase.

Dagger's `HEAD-SNAPSHOT` artifacts can be found at
[https://central.sonatype.com/repository/maven-snapshots/com/google/dagger/](https://central.sonatype.com/repository/maven-snapshots/com/google/dagger/).

We don't recommended using the `HEAD-SNAPSHOT` artifacts in production, but it
can be used to test out, verify, or just give feedback on features that are not
yet released.

## Dagger `HEAD-SNAPSHOT` setup (for Gradle users) {#head-snapshot-setup}

The setup for using the `HEAD-SNAPSHOT` version can be a bit tricky, so this
section walks you through it.

First, replace all of your Dagger dependencies with the `HEAD-SNAPSHOT` version.
For example:

```groovy
dependencies {
    implementation "com.google.dagger:dagger:HEAD-SNAPSHOT"
    annotationProcessor "com.google.dagger:dagger-compiler:HEAD-SNAPSHOT"
}
```

Next, update your plugin and project repositories to include the Sonatype
snapshot url (see
https://central.sonatype.org/publish/publish-portal-snapshots/#consuming-via-gradle).
This usually goes in your top-level `build.gradle` file. For example:

```groovy
// top-level build.gradle file
buildscript {
    repositories {
        maven {
            url "https://central.sonatype.com/repository/maven-snapshots"
        }
    }
}
allprojects {
    repositories {
        maven {
            url "https://central.sonatype.com/repository/maven-snapshots"
        }
    }
}
```

Finally, due to
[Gradle's versioning rules](https://docs.gradle.org/current/userguide/single_versions.html#version_ordering),
you'll also want to add a resolution strategy to ensure the `HEAD-SNAPSHOT` is
not accidentally replaced by a numbered version of Dagger in your transitive
dependencies. This setup usually goes in your application-level `build.gradle`
file. For example:

```groovy
// app-level build.gradle file
configurations.all {
    resolutionStrategy.eachDependency { DependencyResolveDetails details ->
        if (details.requested.group == 'com.google.dagger') {
            details.useVersion "HEAD-SNAPSHOT"
        }
    }
}
```


