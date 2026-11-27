package de.klg71.keycloakmigration.changeControl.actions.workflow

import de.klg71.keycloakmigration.changeControl.actions.Action
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.errorMessage
import de.klg71.keycloakmigration.keycloakapi.isSuccessful
import de.klg71.keycloakmigration.keycloakapi.model.Workflow
import de.klg71.keycloakmigration.keycloakapi.realmExistsById
import de.klg71.keycloakmigration.keycloakapi.workflowByName

/**
 * Deletes a workflow. Keycloak removes every resource's progress through the workflow with it.
 *
 * Undo recreates the workflow from its definition. The progress cannot be restored: resources
 * start the workflow again from the beginning, when its event or schedule next picks them up.
 */
class DeleteWorkflowAction(
    realm: String? = null,
    private val name: String
) : Action(realm) {

    private lateinit var deleted: Workflow

    override fun execute() {
        if (!client.realmExistsById(realm()))
            throw MigrationException("Realm with id: ${realm()} does not exist!")

        val existing = client.workflowByName(name, realm())
            ?: throw MigrationException("Workflow with name: $name does not exist in realm: ${realm()}!")
        deleted = client.workflow(realm(), existing.id!!).definition()

        client.deleteWorkflow(realm(), existing.id!!).run {
            if (!isSuccessful())
                throw MigrationException(
                    "Failed to delete workflow: $name in realm: ${realm()}. " +
                        "Status: ${status()}, Reason: ${errorMessage()}"
                )
        }
    }

    override fun undo() {
        client.createWorkflow(realm(), deleted).run {
            if (!isSuccessful())
                throw MigrationException(
                    "Failed to recreate workflow: $name in realm: ${realm()} during undo. " +
                        "Status: ${status()}, Reason: ${errorMessage()}"
                )
        }
    }

    override fun name() = "DeleteWorkflowAction $name"
}
