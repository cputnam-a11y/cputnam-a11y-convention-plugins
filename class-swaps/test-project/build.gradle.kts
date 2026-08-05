plugins {
    id("java-library")
    id("io.github.cputnama11y.gradle.conventions.class-swaps") version "0.0.1"
    application
}

classSwaps {
    apply(sourceSets["main"])
    globalSwaps.put("io/github/cputnama11y/gradle/conventions/classswaps/test/TestSwap", "java/lang/String")
}

application {
    mainClass = "io.github.cputnama11y.gradle.conventions.classswaps.test.Main"
}