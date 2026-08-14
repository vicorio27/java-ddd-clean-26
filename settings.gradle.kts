plugins {
    // Permite que Gradle descargue el JDK 26 declarado en el toolchain
    // si la máquina (o el runner de CI) no lo tiene instalado.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "sandbox-java26"

include(
    "shared-kernel",
    "domain-orders",
    "domain-payments",
    "domain-inventory",
    "domain-customers",
    "domain-notifications",
    "application",
    "infrastructure-persistence",
    "infrastructure-kafka",
    "infrastructure-llm",
    "infrastructure-notifications",
    "infrastructure-payment-gateway",
    "app",
    "architecture-tests",
)
