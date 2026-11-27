package de.klg71.keycloakmigration.keycloakapi.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A workflow as served by /admin/realms/{realm}/workflows (Keycloak 26.4+).
 *
 * The JSON field names match the workflow definition's YAML keys, so a changelog definition maps
 * onto this class directly. Server-side fields (the workflow and step ids, a step's execution
 * status and the workflow's error state) are ignored on read and never sent back.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Workflow(
    val id: String? = null,
    val name: String,
    val on: String? = null,
    @JsonProperty("if")
    val condition: String? = null,
    val schedule: WorkflowSchedule? = null,
    val concurrency: WorkflowConcurrency? = null,
    val enabled: Boolean? = null,
    val steps: List<WorkflowStep> = emptyList()
) {
    /** The definition without its server-assigned id, as accepted by create and update. */
    fun definition() = copy(id = null)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowSchedule(
    val after: String? = null,
    @JsonProperty("batch-size")
    val batchSize: Int? = null
)

/**
 * Keycloak models both flags as strings ("true"), so they are kept as strings here. A YAML
 * boolean in a changelog is coerced to its string form when the action is read.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowConcurrency(
    @JsonProperty("restart-in-progress")
    val restartInProgress: String? = null,
    @JsonProperty("cancel-in-progress")
    val cancelInProgress: String? = null
)

/**
 * One step of a workflow. `with` holds the step's parameters: Keycloak accepts a single value or
 * a list for each key, and returns a single value when the list has one element.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class WorkflowStep(
    val uses: String,
    val after: String? = null,
    val with: Map<String, Any>? = null
)
