plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    api(project(":domain-payments"))

    implementation(libs.spring.boot.starter.web)
}
