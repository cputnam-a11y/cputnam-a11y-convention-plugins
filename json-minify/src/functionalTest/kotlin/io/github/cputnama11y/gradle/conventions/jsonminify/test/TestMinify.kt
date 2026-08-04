package io.github.cputnama11y.gradle.conventions.jsonminify.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileWriter

class TestMinify {
    @TempDir
    var projectDir: File? = null

    val buildFile
        get() = File(projectDir, "build.gradle.kts")

    val settingsFile
        get() = File(projectDir, "settings.gradle")

    val resourcesFile
        get() = File(projectDir, "src/main/resources/test-resource.json")

    val processedResourcesFile
        get() = File(projectDir, "build/resources/main/test-resource.json")

    @Test
    fun testMinify() {
        writeString(settingsFile, "")
        writeString(
            buildFile,
            """
                plugins {
                    id("io.github.cputnama11y.gradle.conventions.json-minify")
                    java
                }
                
                tasks.processResources {
                    jsonMinify.minify()
                    jsonMinify.report()
                }
            """.trimIndent()
        )
        resourcesFile.parentFile.mkdirs()
        writeString(
            resourcesFile, """
            {
                "test"     : "   testing",
                "testing": [
                
                
                "hi hi hi hi hi"
               
                
                
                ]
            }
        """.trimIndent()
        )

        // Run the build
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withDebug(true)
            .withArguments("processResources")
            .withProjectDir(projectDir)
            .build()
        Assertions.assertTrue(resourcesFile.length() > processedResourcesFile.length())
    }

    private fun writeString(file: File, string: String) {
        FileWriter(file).use { writer ->
            writer.write(string)
        }
    }
}