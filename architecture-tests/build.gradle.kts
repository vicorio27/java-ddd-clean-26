plugins {
    java
}

dependencies {
    testImplementation(platform(libs.spring.boot.dependencies))

    testImplementation(project(":shared-kernel"))
    testImplementation(project(":domain-orders"))
    testImplementation(project(":domain-payments"))
    testImplementation(project(":domain-inventory"))
    testImplementation(project(":domain-customers"))
    testImplementation(project(":domain-notifications"))
    testImplementation(project(":application"))
    testImplementation(project(":infrastructure-persistence"))
    testImplementation(project(":infrastructure-kafka"))
    testImplementation(project(":infrastructure-llm"))
    testImplementation(project(":infrastructure-notifications"))
    testImplementation(project(":infrastructure-payment-gateway"))
    testImplementation(project(":app"))

    testImplementation(libs.archunit.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.spring.boot.starter.web)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// ArchUnit no logra resolver las clases del proyecto por classloader bajo Gradle 9
// (los modulos llegan como jars y el classpath va en un manifest jar). Se le pasan
// las rutas de forma explicita y el test filtra las que pertenecen a este repo.
tasks.test {
    val rootPath = rootDir.absolutePath
    val classpath = sourceSets.test.get().runtimeClasspath
    inputs.files(classpath)
    doFirst {
        systemProperty("sandbox.rootDir", rootPath)
        systemProperty("sandbox.classpath", classpath.asPath)
    }
}
