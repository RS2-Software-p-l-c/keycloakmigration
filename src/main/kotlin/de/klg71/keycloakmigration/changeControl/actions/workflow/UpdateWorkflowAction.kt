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

private const val BAD_REQUEST = 400

/**
 * Replaces a workflow's definition. Like Keycloak's PUT, this is a full replace: fields left out
 * of the change are removed from the workflow, not kept.
 *
 * While any resource is part-way through the workflow, Keycloak only accepts changes to settings
 * (`if`, `schedule`, `concurrency`, `enabled` and each step's `after` and `with`). It refuses a
 * change to `on` or to the number, order or `uses` of the steps. This action reports that refusal
 * instead of deleting and recreating the workflow, because recreating it would drop every
 * resource's progress. To make such a change anyway, use deleteWorkflow followed by addWorkflow.
 *
 * Undo puts the previous definition back.
 */
@Suppress("LongParameterList")
class UpdateWorkflowAction(
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

    private lateinit var id: String
    private lateinit var previous: Workflow

    override fun execute() {
        if (!client.realmExistsById(realm()))
            throw MigrationException("Realm with id: ${realm()} does not exist!")

        val existing = client.workflowByName(name, realm())
            ?: throw MigrationException("Workflow with name: $name does not exist in realm: ${realm()}!")
        id = existing.id!!
        previous = client.workflow(realm(), id).definition()

        val updated = Workflow(
            name = name,
            on = on,
            condition = condition,
            schedule = schedule,
            concurrency = concurrency,
            enabled = enabled,
            steps = steps
        )
        client.updateWorkflow(realm(), id, updated).run {
            if (!isSuccessful()) {
                val message = errorMessage()
                throw MigrationException(
                    "Failed to update workflow: $name in realm: ${realm()}. Status: ${status()}, Reason: $message" +
                        if (status() == BAD_REQUEST && message.contains("scheduled resources"))
                            ". Resources are part-way through this workflow, so its 'on' and its steps cannot " +
                                "change. Use deleteWorkflow followed by addWorkflow to replace it; this drops " +
                                "every resource's progress through the workflow."
                        else ""
                )
            }
        }
    }

    override fun undo() {
        client.updateWorkflow(realm(), id, previous).run {
            if (!isSuccessful())
                throw MigrationException(
                    "Failed to restore workflow: $name in realm: ${realm()} during undo. " +
                        "Status: ${status()}, Reason: ${errorMessage()}"
                )
        }
    }

    override fun name() = "UpdateWorkflowAction $name"
}
