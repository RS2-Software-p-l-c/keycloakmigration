package de.klg71.keycloakmigration.changeControl.actions.workflow

import de.klg71.keycloakmigration.keycloakapi.model.WorkflowConcurrency
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowSchedule
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowStep

internal const val WORKFLOW_NAME = "Disable inactive users"

internal val warnThenDisable = listOf(
    WorkflowStep(uses = "notify-user", after = "85d"),
    WorkflowStep(uses = "disable-user", after = "5d")
)

internal fun addWorkflow(
    realm: String,
    on: String? = "user-created or user-authenticated",
    steps: List<WorkflowStep> = warnThenDisable
) = AddWorkflowAction(
    realm = realm,
    name = WORKFLOW_NAME,
    on = on,
    schedule = WorkflowSchedule(after = "1d", batchSize = 500),
    concurrency = WorkflowConcurrency(restartInProgress = "true"),
    steps = steps
).executeIt()
