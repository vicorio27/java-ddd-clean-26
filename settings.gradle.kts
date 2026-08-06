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
