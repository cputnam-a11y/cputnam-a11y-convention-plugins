plugins {
    `java-gradle-plugin`
    java
    kotlin("jvm") version "2.4.10"
}

gradlePlugin {
    plugins.register("json-minify") {
        this.id = "${project.group as String}.${project.name}"
        this.displayName = "Json Minify"
        this.description = "Minifies Json"
        this.implementationClass = "io.github.cputnama11y.gradle.conventions.jsonminify.JsonMinifyPlugin"
    }
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest")
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.register("functionalTest", Test::class) {
    description = "Run Functional Tests"
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}