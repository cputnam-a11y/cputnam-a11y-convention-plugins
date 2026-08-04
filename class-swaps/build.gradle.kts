plugins {
    `java-gradle-plugin`
    java
    kotlin("jvm") version "2.4.10"
}

dependencies {
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-commons:9.10.1")
}

gradlePlugin {
    plugins.register("class-swaps") {
        this.id = "${project.group as String}.${project.name}"
        this.displayName = "Class Swaps"
        this.description = "Swaps Classes"
        this.implementationClass = "io.github.cputnama11y.gradle.conventions.classswaps.ClassSwapsPlugin"
    }
}