package io.github.cputnama11y.gradle.conventions.classswaps

import org.gradle.api.Plugin
import org.gradle.api.Project

class ClassSwapsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("classSwaps", ClassSwapsExtension::class.java)
    }
}