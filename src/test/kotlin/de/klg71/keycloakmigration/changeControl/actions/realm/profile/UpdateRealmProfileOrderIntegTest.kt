package de.klg71.keycloakmigration.changeControl.actions.realm.profile

import de.klg71.keycloakmigration.AbstractIntegrationTest
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.KeycloakClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.koin.core.component.inject

class UpdateRealmProfileOrderIntegTest : AbstractIntegrationTest() {

    private val client by inject<KeycloakClient>()

    @Test
    fun testReorderAttributes_Success() {
        val orderedAttributes = listOf("lastName", "firstName", "email", "username")

        UpdateRealmProfileOrderAction(
            testRealm,
            orderedAttributes
        ).executeIt()

        val updatedAttributes = client.realmUserProfile(testRealm).attributes
        val updatedNames = updatedAttributes.map { it.name }

        assertThat(updatedNames).isEqualTo(orderedAttributes)
    }

    @Test(expected = MigrationException::class)
    fun testReorderAttributes_AttributeInOrderNotInProfile_Throws() {
        val invalidOrder = listOf("lastName", "nonExistentAttr", "email", "username")
        UpdateRealmProfileOrderAction(testRealm, invalidOrder).executeIt()
    }

    @Test(expected = MigrationException::class)
    fun testReorderAttributes_ProfileHasExtraAttr_Throws() {
        val profileAttributes = client.realmUserProfile(testRealm).attributes.map { it.name }
        val missingOrder = profileAttributes.drop(1)
        UpdateRealmProfileOrderAction(testRealm, missingOrder).executeIt()
    }
}
