package io.github.cputnama11y.gradle.conventions.jsonminify

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.language.jvm.tasks.ProcessResources

class JsonMinifyPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.withId("java") {
            target.tasks.withType(ProcessResources::class.java).configureEach {
                (it as? ExtensionAware)?.extensions?.create(
                    "jsonMinify",
                    JsonMinifyExtension::class.java,
                    target,
                    it,
                    JsonStatsTracker()
                )
            }
        }
    }
}