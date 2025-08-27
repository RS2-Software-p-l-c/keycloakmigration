package de.klg71.keycloakmigration.changeControl.actions.organization

import de.klg71.keycloakmigration.changeControl.actions.Action
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.model.OrganizationDomain
import de.klg71.keycloakmigration.keycloakapi.model.UpdateOrganization
import de.klg71.keycloakmigration.keycloakapi.organizationByName
import de.klg71.keycloakmigration.keycloakapi.realmExistsById
import de.klg71.keycloakmigration.keycloakapi.editOrganization

class UpdateOrganizationAction(
    realm: String?,
    private val name: String,
    private val redirectUrl: String? = null,
    private var domains: Set<OrganizationDomain>?,
    private val attributes: Map<String, List<String>>? = mapOf()
) : Action(realm) {

    override fun execute() {
        if (!client.realmExistsById(realm()))
            throw MigrationException("Realm with id: ${realm()} does not exist!")

        val organization = client.organizationByName(name, realm())

        val orgId = organization.id

        val updatedOrganization = UpdateOrganization(
            name,
            organization.alias,
            redirectUrl = redirectUrl ?: organization.redirectUrl,
            domains = domains?.ifEmpty { organization.domains },
            attributes = attributes ?: organization.attributes
        )

        client.editOrganization(realm(), orgId, updatedOrganization)
    }

    override fun undo() {
        client.organizationByName(name, realm()).run {
            client.deleteOrganization(id, realm())
        }
    }

    override fun name() = "UpdateOrganizationAction $name"
}