package de.klg71.keycloakmigration.keycloakapi.model

data class UpdateOrganization(
    val name: String,
    val alias: String?,
    val redirectUrl: String?,
    val domains: Set<OrganizationDomain>? = setOf(),
    val attributes: Map<String, List<String>>? = mapOf()
)
