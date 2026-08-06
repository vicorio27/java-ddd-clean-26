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
}
