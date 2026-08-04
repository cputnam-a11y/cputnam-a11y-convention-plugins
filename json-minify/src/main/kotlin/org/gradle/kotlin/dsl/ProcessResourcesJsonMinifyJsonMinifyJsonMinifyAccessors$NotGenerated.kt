package org.gradle.kotlin.dsl

import io.github.cputnama11y.gradle.conventions.jsonminify.JsonMinifyExtension
import org.gradle.language.jvm.tasks.ProcessResources

fun ProcessResources.jsonMinify(): JsonMinifyExtension = this.extensions.getByType(JsonMinifyExtension::class.java)
val ProcessResources.jsonMinify: JsonMinifyExtension
    get() = this.extensions.getByType(JsonMinifyExtension::class.java)
fun ProcessResources.jsonMinify(configure: JsonMinifyExtension.() -> Unit) = jsonMinify.configure()
