package de.klg71.keycloakmigration.changeControl.actions.workflow

import com.fasterxml.jackson.annotation.JsonProperty
import de.klg71.keycloakmigration.changeControl.actions.Action
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.errorMessage
import de.klg71.keycloakmigration.keycloakapi.isSuccessful
import de.klg71.keycloakmigration.keycloakapi.model.Workflow
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowConcurrency
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowSchedule
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowStep
import de.klg71.keycloakmigration.keycloakapi.realmExistsById
import de.klg71.keycloakmigration.keycloakapi.workflowByName

/**
 * Creates a workflow. Fails if a workflow with the same name already exists in the realm.
 * Undo deletes the workflow it created.
 */
@Suppress("LongParameterList")
class AddWorkflowAction(
    realm: String? = null,
    private val name: String,
    private val on: String? = null,
    @JsonProperty("if")
    private val condition: String? = null,
    private val schedule: WorkflowSchedule? = null,
    private val concurrency: WorkflowConcurrency? = null,
    private val enabled: Boolean? = null,
    private val steps: List<WorkflowStep>
) : Action(realm) {

    override fun execute() {
        if (!client.realmExistsById(realm()))
            throw MigrationException("Realm with id: ${realm()} does not exist!")

        if (client.workflowByName(name, realm()) != null)
            throw MigrationException("Workflow with name: $name already exists in realm: ${realm()}!")

        val workflow = Workflow(
            name = name,
            on = on,
            condition = condition,
            schedule = schedule,
            concurrency = concurrency,
            enabled = enabled,
            steps = steps
        )
        client.createWorkflow(realm(), workflow).run {
            if (!isSuccessful())
                throw MigrationException(
                    "Failed to add workflow: $name in realm: ${realm()}. Status: ${status()}, Reason: ${errorMessage()}"
                )
        }
    }

    override fun undo() {
        val created = client.workflowByName(name, realm()) ?: return
        client.deleteWorkflow(realm(), created.id!!).run {
            if (!isSuccessful())
                throw MigrationException(
                    "Failed to delete workflow: $name in realm: ${realm()} during undo. " +
                        "Status: ${status()}, Reason: ${errorMessage()}"
                )
        }
    }

    override fun name() = "AddWorkflowAction $name"
}
