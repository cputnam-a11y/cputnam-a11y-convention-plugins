```kts
// 1. add the repository to the pluginManagement block in settings.gradle.kts
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.cp-a11y.dev/")
            name = "cp-a11y maven"
        }
    }
}
```

```kts
plugins {
    // 2. apply desired plugins to the project
    id("io.github.cputnama11y.gradle.conventions.json-minify") version "0.0.1"
    id("io.github.cputnama11y.gradle.conventions.class-swaps") version "0.0.1"
}

// 3. provide plugin specific configuration

tasks.processResources {
    // minify and log all json in the processResources task
    jsonMinify.minify()
    jsonMinify.report()
}

classSwaps {
    apply(sourceSets["main"]).configure {
        swaps.put("com/example/SomeTestSwapClass", "java/lang/Class") // swap all usages of SomeOtherTestSwapClass for String after compiling in only this SourceSet
    }
    
    globalSwaps.put("com/example/SomeOtherTestSwapClass", "java/lang/String") // swap all usages of SomeOtherTestSwapClass for String after compiling in all applied SourceSets
}
```