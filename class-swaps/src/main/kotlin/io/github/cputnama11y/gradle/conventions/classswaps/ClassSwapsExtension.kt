package io.github.cputnama11y.gradle.conventions.classswaps


import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.PublishArtifactSet
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import java.util.function.Function
import javax.inject.Inject

abstract class ClassSwapsExtension @Inject constructor(private val project: Project) : ExtensionAware {
    abstract val globalSwaps: MapProperty<String, String>
    fun apply(
        sourceSet: SourceSet,
        sourceDirectorySet: SourceDirectorySet,
        compileTaskProvider: TaskProvider<out AbstractCompile>
    ): TaskProvider<ClassSwapsTask> {
        return apply(
            sourceSet,
            sourceDirectorySet,
            compileTaskProvider,
            AbstractCompile::getDestinationDirectory
        )
    }

    fun <T : Task> apply(
        sourceSet: SourceSet,
        sourceDirectorySet: SourceDirectorySet,
        compileTaskProvider: TaskProvider<T>,
        mapping: Function<T, DirectoryProperty>
    ): TaskProvider<ClassSwapsTask> {
        val unprocessed: Provider<Directory> = project.layout.buildDirectory.dir(
            "classSwaps/unprocessed/${compileTaskProvider.name}"
        )
        compileTaskProvider.configure { mapping.apply(it).set(unprocessed) }
        val capitalized =
            compileTaskProvider.name.substring(0, 1).uppercase() + compileTaskProvider.name.substring(1)
        val openSesameTask = project.tasks.register("classSwaps$capitalized", ClassSwapsTask::class.java) {
            it.inputClasses.set(mapping.apply(compileTaskProvider.get()))
            it.outputClasses.set(sourceDirectorySet.classesDirectory)
            it.swaps.putAll(globalSwaps)
        }
        sourceDirectorySet.compiledBy(openSesameTask, ClassSwapsTask::outputClasses::get)
        (sourceSet.output.classesDirs as ConfigurableFileCollection).builtBy(openSesameTask)
        project.tasks.named(sourceSet.classesTaskName).configure {
            it.dependsOn(openSesameTask)
        }


        // Fix classes secondary variant...
        project.configurations.configureEach { c ->
            c.outgoing.variants.configureEach { v ->
                replaceArtifacts(openSesameTask, v.artifacts, c.name, c.artifacts)
            }
            replaceArtifacts(openSesameTask, c.artifacts, c.name, c.artifacts)
        }

        return openSesameTask
    }

    private fun replaceArtifacts(
        classSwapsTask: TaskProvider<ClassSwapsTask>,
        artifacts: PublishArtifactSet,
        name: String,
        delegate: PublishArtifactSet
    ) {
        val path = classSwapsTask.get().outputClasses.get().asFile.toPath().toAbsolutePath()
        val matching: DomainObjectSet<PublishArtifact> = artifacts.matching {
            it.file.toPath().toAbsolutePath() == path
        }
        val removed = ArrayList<PublishArtifact>(matching)
        removed.forEach(artifacts::remove)
        for (artifact in removed) {
            val created = project.artifacts.add(name, path.toFile()) {
                it.builtBy(classSwapsTask)
                it.extension = artifact.extension
                it.classifier = artifact.classifier
                it.type = artifact.type
                it.name = artifact.name
            }
            delegate.remove(created)
            artifacts.add(created)
        }
    }

    fun apply(sourceSet: SourceSet): TaskProvider<ClassSwapsTask> {
        return apply(
            sourceSet,
            sourceSet.java,
            project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java)
        )
    }
}