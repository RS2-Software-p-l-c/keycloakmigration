package de.klg71.keycloakmigration.changeControl.actions.client

import de.klg71.keycloakmigration.AbstractIntegrationTest
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.changeControl.actions.role.AddRoleAction
import de.klg71.keycloakmigration.keycloakapi.KeycloakClient
import de.klg71.keycloakmigration.keycloakapi.clientById
import de.klg71.keycloakmigration.keycloakapi.model.RoleListItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.koin.core.component.inject
import java.util.UUID

class RevokeRoleFromClientIntegTest : AbstractIntegrationTest() {

    private val client by inject<KeycloakClient>()
    private val clientId = "clientId"
    private val role = "testRole"
    private val roleClientId = "roleClientId"

    @Test
    fun testRevokeRole() {
        AddSimpleClientAction(testRealm, clientId, serviceAccountsEnabled = true).executeIt()
        AddRoleAction(testRealm, role).executeIt()
        AssignRoleToClientAction(testRealm, role, clientId).executeIt()
        RevokeRoleFromClientAction(testRealm, role, clientId).executeIt()

        val role = RoleListItem(
            UUID.randomUUID(), role, null,
            composite = false,
            clientRole = false,
            containerId = testRealm
        )

        val actual = client.clientRoles(testRealm, client.clientById(clientId, testRealm).id)

        assertThat(actual).doesNotContain(role);

    }

    @Test
    fun testRevokeRole_clientNotExisting() {
        AddRoleAction(testRealm, role).executeIt()
        assertThatThrownBy {
            RevokeRoleFromClientAction(testRealm, role, clientId).executeIt()
        }.isInstanceOf(MigrationException::class.java)
            .hasMessage("Client with name: $clientId does not exist in realm: $testRealm!")
    }

}
