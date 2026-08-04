package io.github.cputnama11y.gradle.conventions.jsonminify

import org.gradle.api.Project
import org.gradle.language.jvm.tasks.ProcessResources

open class JsonMinifyExtension(val project: Project, val processResources: ProcessResources, val tracker: JsonStatsTracker) {
    fun minify() {
        processResources.filesMatching("**.json") {
            it.filter(mapOf<String, Any>("tracker" to tracker), JsonMinifyingReader::class.java)
        }
    }

    fun report() {
        processResources.doLast {
            project.logger.lifecycle("Minified ${tracker.jsonMinified} files. Saved ${tracker.jsonBytesSaved} bytes before compression. Took ${tracker.jsonTotalTime}ms.")
        }
    }
}