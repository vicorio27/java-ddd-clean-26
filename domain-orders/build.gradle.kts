plugins {
    `java-library`
}

dependencies {
    api(project(":shared-kernel"))

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
