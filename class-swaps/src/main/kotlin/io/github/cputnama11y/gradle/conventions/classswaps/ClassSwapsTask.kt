/*
This class adapted from OpenSesameTask.java from OpenSesame (https://github.com/lukebemishprojects/OpenSesame), originally under the BSD 3-Clause License:
BSD 3-Clause License

Copyright (c) 2023, Luke Bemish

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.github.cputnama11y.gradle.conventions.classswaps

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.inject.Inject

@CacheableTask
abstract class ClassSwapsTask @Inject constructor() : DefaultTask() {
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    @get:Incremental
    abstract val inputClasses: DirectoryProperty

    @get:Input
    abstract val swaps: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputClasses: DirectoryProperty

    @get:LocalState
    abstract val incrementalClasses: RegularFileProperty

    @get:LocalState
    abstract val previousSwapsFile: RegularFileProperty

    private var previousSwaps: Map<String, String>
        get() = previousSwapsFile.get().asFile.toPath().run {
            if (Files.exists(this)) mapOf(
                *Files.readAllLines(previousSwapsFile.get().asFile.toPath()).map {
                    it.split(":").let {
                        if (it.size != 2) throw IllegalStateException("expected swapKey:swapValue")
                        else it[0] to it[1]
                    }
                }.toTypedArray()
            ) else emptyMap()
        }
        set(value) {
            Files.write(
                previousSwapsFile.get().asFile.also {
                    Files.createDirectories(it.parentFile.toPath())
                }.toPath(),
                value.entries.map { (k, v) -> "$k:$v" },
                StandardOpenOption.CREATE
            )
        }

    init {
        this.incrementalClasses.convention(
            project.layout.buildDirectory.file("classSwaps/incrementalClasses/$name")
        )
        this.previousSwapsFile.convention(
            project.layout.buildDirectory.file("classSwaps/previousSwapsCache/$name")
        )
    }

    @TaskAction
    @Throws(IOException::class)
    protected fun process(changes: InputChanges) {
        val outputDir = this.outputClasses.get().asFile.toPath()
        val inputDir = this.inputClasses.get().asFile.toPath()
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir)
        }

        val toProcess: MutableSet<String> = LinkedHashSet()
        val incrementalClassesFile = this.incrementalClasses.get().asFile

        if (previousSwaps == swaps.get()) {
            if (incrementalClassesFile.exists()) {
                toProcess.addAll(Files.readAllLines(incrementalClassesFile.toPath()))
            }
            changes.getFileChanges(this.inputClasses).forEach { change ->
                val path = change.file.toPath()
                val relativePath = inputDir.relativize(path).toString()
                val outputPath = outputDir.resolve(relativePath)
                if (change.changeType == ChangeType.REMOVED) {
                    toProcess.remove(relativePath)
                    if (Files.exists(outputPath)) {
                        try {
                            Files.delete(outputPath)
                        } catch (e: IOException) {
                            throw RuntimeException(e)
                        }
                    }
                } else {
                    toProcess.add(relativePath)
                }
            }
        } else {
            Files.walk(inputDir)
                .filter(Files::isRegularFile)
                .forEach {
                    Files.deleteIfExists(outputDir.resolve(inputDir.relativize(it)))
                    toProcess += inputDir.relativize(it).toString()
                }
        }

        val processed: MutableList<String> = ArrayList()
        for (relativePath in toProcess) {
            val inputPath = inputDir.resolve(relativePath)
            if (Files.exists(inputPath)) {
                val outputPath = outputDir.resolve(relativePath)
                Files.createDirectories(outputPath.parent)
                var changed = false
                val swaps = LinkedHashMap(swaps.get())
                val remapper = object : Remapper(Opcodes.ASM9) {
                    override fun map(internalName: String): String {
                        val visited = mutableSetOf<String>()
                        var newName = internalName
                        while (swaps[newName]?.let { newName = it } != null) {
                            if (!visited.add(newName)) throw IllegalStateException("Cycle detected in wasps")
                        }
                        changed = true
                        return newName
                    }
                }
                val writer = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
                ClassReader(Files.readAllBytes(inputPath)).accept(
                    object : ClassRemapper(writer, remapper) {
                        override fun visit(
                            version: Int,
                            access: Int,
                            name: String,
                            signature: String?,
                            superName: String,
                            interfaces: Array<out String>
                        ) {
                            val temp = swaps.remove(name)
                            super.visit(version, access, name, signature, superName, interfaces)
                            swaps[name] = temp
                        }
                    },
                    0
                )
                Files.write(outputPath, writer.toByteArray(), StandardOpenOption.CREATE)
                if (changed) processed.add(relativePath)
            }
        }
        Files.write(incrementalClassesFile.toPath(), processed)
        previousSwaps = swaps.get()
    }
}