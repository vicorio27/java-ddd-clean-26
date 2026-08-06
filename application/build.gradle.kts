plugins {
    `java-library`
}

dependencies {
    api(project(":domain-orders"))
    api(project(":domain-payments"))
    api(project(":domain-inventory"))
    api(project(":domain-customers"))
    api(project(":domain-notifications"))
}
