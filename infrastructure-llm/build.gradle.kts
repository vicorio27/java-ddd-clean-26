plugins {
    `java-library`
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))

    api(project(":application"))

    implementation(libs.spring.boot.starter.web)
}
