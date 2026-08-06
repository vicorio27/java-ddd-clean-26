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

    testImplementation(libs.spring.boot.starter.test)
}
