package de.klg71.keycloakmigration.changeControl.actions.client

import de.klg71.keycloakmigration.changeControl.actions.Action
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.clientRoleByName
import de.klg71.keycloakmigration.keycloakapi.clientUUID
import de.klg71.keycloakmigration.keycloakapi.existsClient
import de.klg71.keycloakmigration.keycloakapi.existsRole
import de.klg71.keycloakmigration.keycloakapi.model.RoleListItem
import de.klg71.keycloakmigration.keycloakapi.model.AssignRole

class RevokeRoleFromClientAction(
    realm: String? = null,
    private val role: String,
    private val clientId: String,
    private val roleClientId: String? = null
) : Action(realm) {

    override fun execute() {
        if (!client.existsClient(clientId, realm()))
            throw MigrationException("Client with name: $clientId does not exist in realm: ${realm()}!")

        val foundRole = findRole()
        val roleListItem = RoleListItem(
            id = foundRole.id,
            name = foundRole.name,
            description = foundRole.description,
            composite = foundRole.composite,
            clientRole = foundRole.clientRole,
            containerId = foundRole.containerId
        )
        val serviceAccountUser = client.clientServiceAccount(client.clientUUID(clientId, realm()), realm())

        if (roleClientId != null) {

            val clientOfRoleUUID = client.clientUUID(roleClientId, realm())

            val assignedClientRolesToServiceAccount = client.userClientRoles(
                realm(),
                serviceAccountUser.id, clientOfRoleUUID
            )
            if (!assignedClientRolesToServiceAccount.map { it.name }.contains(role)) {
                throw MigrationException(
                    "Service account for client '$clientId' in realm: ${realm()} does not have client role: $role from client: $roleClientId!"
                )
            }
            client.revokeClientRoles(
                listOf(roleListItem.toAssignRole()),
                realm(),
                serviceAccountUser.id,
                clientOfRoleUUID
            )

        } else {
            if (!client.existsRole(role, realm()))
                throw MigrationException("Realm role with name: $role does not exist in realm: ${realm()}!")

            // TODO: Discuss: findRealmRolesAssignedToClient does not exist - meaning we can't validate that a client has the role before attempting to remove it
            client.revokeRealmRoles(
                listOf(roleListItem.toAssignRole()),
                realm(),
                serviceAccountUser.id)

        }
    }

    override fun undo() {
        val foundRole = findRole()
        val roleListItem = RoleListItem(
            id = foundRole.id,
            name = foundRole.name,
            description = foundRole.description,
            composite = foundRole.composite,
            clientRole = foundRole.clientRole,
            containerId = foundRole.containerId
        )

        if (roleClientId != null) {
            val serviceAccountUser = client.clientServiceAccount(client.clientUUID(clientId, realm()), realm())

            client.assignClientRoles(
                listOf(roleListItem.toAssignRole()),
                realm(),
                serviceAccountUser.id,
                client.clientUUID(roleClientId, realm())
            )
        } else {
            client.assignRealmRoles(
                listOf(roleListItem.toAssignRole()),
                realm(),
                client.clientUUID(clientId, realm())
            )
        }
    }

    private fun findRole() = if (roleClientId == null) {
        client.roleByName(role, realm())
    } else {
        client.clientRoleByName(role, roleClientId, realm())
    }

    private fun RoleListItem.toAssignRole(): AssignRole {
        return AssignRole(
            id = this.id,
            name = this.name,
            composite = this.composite,
            clientRole = this.clientRole,
            containerId = this.containerId
        )
    }

    override fun name() = "Revoke Role $role from Client: $clientId"
}