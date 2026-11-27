package de.klg71.keycloakmigration.changeControl.actions.realm.profile

import de.klg71.keycloakmigration.AbstractIntegrationTest
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.KeycloakClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.koin.core.component.inject

class DeleteRealmProfileAttributeIntegTest : AbstractIntegrationTest() {

    val client by inject<KeycloakClient>()

    private fun attributeNames() = client.realmUserProfile(testRealm).attributes.map { it.name }

    @Test
    fun testDeleteRealmProfileAttribute() {
        AddRealmProfileAttributeAction(testRealm, name = "country").executeIt()

        DeleteRealmProfileAttributeAction(testRealm, "country").executeIt()

        assertThat(attributeNames()).doesNotContain("country")
    }

    @Test
    fun testDeleteMissingRealmProfileAttribute() {
        assertThatThrownBy { DeleteRealmProfileAttributeAction(testRealm, "country").executeIt() }
            .isInstanceOf(MigrationException::class.java)
            .hasMessage("Realm profile attribute with name: country does not exist!")
    }

    @Test
    fun testUndoDeleteRealmProfileAttributeRestoresItsPosition() {
        AddRealmProfileAttributeAction(testRealm, name = "country").executeIt()
        AddRealmProfileAttributeAction(testRealm, name = "station").executeIt()
        UpdateRealmProfileOrderAction(
            testRealm,
            listOf("username", "email", "country", "firstName", "lastName", "station")
        ).executeIt()
        val before = attributeNames()
        val action = DeleteRealmProfileAttributeAction(testRealm, "country")
        action.executeIt()

        action.undoIt()

        assertThat(attributeNames()).isEqualTo(before)
    }
}
