plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    api(project(":domain-orders"))
    implementation(project(":domain-notifications"))

    implementation(libs.spring.kafka)
    implementation(libs.spring.boot.starter.web)
}
