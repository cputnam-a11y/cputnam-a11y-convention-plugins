package io.github.cputnama11y.gradle.conventions.classswaps

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.work.ChangeType
import org.gradle.work.FileChange
import org.gradle.work.InputChanges
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.IOException
import java.nio.file.Files
import java.util.function.Consumer
import javax.inject.Inject

//TODO use previous swaps in file to compare for all recomp
//@CacheableTask
abstract class ClassSwapsTask @Inject constructor() : DefaultTask() {
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    abstract val inputClasses: DirectoryProperty

    @get:Input
    abstract val swaps: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputClasses: DirectoryProperty

    @get:LocalState
    abstract val incrementalClasses: RegularFileProperty

    init {
        this.incrementalClasses.convention(
            project.layout.buildDirectory.file("classSwaps/incrementalClasses/$name")
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
        if (incrementalClassesFile.exists()) {
            toProcess.addAll(Files.readAllLines(incrementalClassesFile.toPath()))
        }
        changes.getFileChanges(this.inputClasses).forEach(Consumer { change: FileChange? ->
            val path = change!!.file.toPath()
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
        })
//        VisitingProcessor.cleanup(outputDir)
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
                    }, 0)
                Files.write(outputPath, writer.toByteArray())
                if (changed) processed.add(relativePath)
            }
        }
        Files.write(incrementalClassesFile.toPath(), processed)
    }
}