plugins {
    java
}

allprojects {
    group = "com.sandbox"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(26))
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters"))
        // Se compila CON el JDK 26 pero se emite bytecode 25. Motivo concreto: el ASM que
        // empaqueta ArchUnit 1.4.1 solo lee hasta class file V25, asi que con bytecode V26
        // descarta todas las clases en silencio y las reglas de arquitectura pasan en vacio.
        // Revisar en cada subida de ArchUnit.
        options.release.set(25)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
