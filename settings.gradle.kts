@file:Suppress("UnstableApiUsage")


rootProject.name = "convention-plugins"

include("json-minify", "class-swaps")

dependencyResolutionManagement {
    repositories {
        repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
        mavenCentral()
    }
}
gradle.lifecycle.beforeProject {
    plugins.apply("maven-publish")

    providers.gradleProperty("$name.version").apply {
        if (isPresent) version = get()
    }

    providers.gradleProperty("maven.group").apply {
        if (isPresent) group = get()
    }
    project.plugins.withId("java") {
        this@beforeProject.extensions.getByType(PublishingExtension::class.java).apply {
            repositories {
                maven {
                    url = uri("https://repo.repsy.io/cputnam-a11y/maven")
                    name = "cputnama11yMaven"
                    credentials(PasswordCredentials::class)
                }
            }

            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }
        }
    }
}