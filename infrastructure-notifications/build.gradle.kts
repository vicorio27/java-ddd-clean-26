plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    api(project(":domain-notifications"))

    implementation(libs.spring.boot.starter.web)
}
