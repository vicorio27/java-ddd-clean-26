plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":application"))
    implementation(project(":infrastructure-persistence"))
    implementation(project(":infrastructure-kafka"))
    implementation(project(":infrastructure-llm"))
    implementation(project(":infrastructure-notifications"))
    implementation(project(":infrastructure-payment-gateway"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.datatype.jsr310)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.jdbc)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.awaitility)
    testRuntimeOnly(libs.junit.platform.launcher)
}
