package de.klg71.keycloakmigration.changeControl.actions.realm.profile

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.klg71.keycloakmigration.changeControl.actions.Action
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.errorMessage
import de.klg71.keycloakmigration.keycloakapi.isSuccessful
import de.klg71.keycloakmigration.keycloakapi.model.RealmProfile
import de.klg71.keycloakmigration.keycloakapi.realmExistsById

/**
 * Removes an attribute from the realm's user profile. Values already stored on users are not
 * touched; whether they stay visible depends on the realm's unmanaged attribute policy.
 *
 * Undo puts the previous user profile back, with the attribute at its original position.
 */
class DeleteRealmProfileAttributeAction(
    realm: String? = null,
    private val name: String
) : Action(realm) {

    private lateinit var oldRealmProfile: RealmProfile

    override fun execute() {
        if (!client.realmExistsById(realm()))
            throw MigrationException("Realm with id: ${realm()} does not exist!")

        val realmProfile = client.realmUserProfile(realm())

        val mapper = jacksonObjectMapper()
        oldRealmProfile = mapper.readValue(mapper.writeValueAsString(realmProfile), RealmProfile::class.java)

        if (!realmProfile.attributes.removeIf { it.name == name })
            throw MigrationException("Realm profile attribute with name: $name does not exist!")

        client.updateRealmProfile(realm(), realmProfile).run {
            if (!isSuccessful())
                throw MigrationException(
                    "Failed to delete realm profile attribute: $name in realm: ${realm()}. " +
                        "Status: ${status()}, Reason: ${errorMessage()}"
                )
        }
    }

    override fun undo() {
        client.updateRealmProfile(realm(), oldRealmProfile)
    }

    override fun name() = "DeleteRealmProfileAttributeAction $name"
}
