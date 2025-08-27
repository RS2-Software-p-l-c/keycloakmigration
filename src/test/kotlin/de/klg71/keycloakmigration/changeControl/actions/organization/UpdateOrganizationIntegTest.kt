package de.klg71.keycloakmigration.changeControl.actions.organization

import de.klg71.keycloakmigration.AbstractIntegrationTest
import de.klg71.keycloakmigration.changeControl.actions.realm.UpdateRealmAction
import de.klg71.keycloakmigration.keycloakapi.KeycloakClient
import de.klg71.keycloakmigration.keycloakapi.model.OrganizationDomain
import de.klg71.keycloakmigration.keycloakapi.organizationByName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.koin.core.component.inject
import kotlin.getValue

class UpdateOrganizationIntegTest : AbstractIntegrationTest() {

    val client by inject<KeycloakClient>()

    @Test
    fun testUpdateOrganization() {
        val name = "test"

        UpdateRealmAction(testRealm, organizationsEnabled = true).executeIt()
        val addOrganizationAction = AddOrganizationAction(
            testRealm,
            name,
            alias = "alias",
            redirectUrl = "http://redirectUrl.com",
            domains = setOf(OrganizationDomain("test.com")),
            attributes = mapOf("custom-attribute" to listOf("values"))
        )
        addOrganizationAction.executeIt()

        UpdateOrganizationAction(
            testRealm,
            name,
            redirectUrl = "http://updatedRedirectUrl.com",
            domains = setOf(OrganizationDomain("updated-test.com")),
            attributes = mapOf("custom-attribute" to listOf("updated-values"))
        ).executeIt()

        val updatedOrg = client.organizationByName(name, testRealm)

        assertThat(updatedOrg.name).isEqualTo(name)
        assertThat(updatedOrg.redirectUrl).isEqualTo("http://updatedRedirectUrl.com")
        assertThat(updatedOrg.domains).isEqualTo(setOf(OrganizationDomain("updated-test.com")))
        assertThat(updatedOrg.attributes).isEqualTo(mapOf("custom-attribute" to listOf("updated-values")))
    }
}