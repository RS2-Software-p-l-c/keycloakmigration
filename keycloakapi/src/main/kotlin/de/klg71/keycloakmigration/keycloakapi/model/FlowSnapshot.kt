package de.klg71.keycloakmigration.keycloakapi.model

import java.util.UUID

/**
 * A flow's executions as a tree, as read before a change so the change can be undone. Unlike the flat
 * list Keycloak returns, it keeps sub flows with their settings and the executions nested in them.
 */
sealed interface FlowNode {
    val requirement: Flow.Requirement
    val priority: Int
}

data class ExecutionNode(
    val providerId: String?,
    override val requirement: Flow.Requirement,
    override val priority: Int,
    val config: AuthenticatorConfig? = null
) : FlowNode

data class SubFlowNode(
    val alias: String,
    val description: String,
    val type: String,
    val providerId: String?,
    override val requirement: Flow.Requirement,
    override val priority: Int,
    val executions: List<FlowNode>
) : FlowNode

/** The parts of a flow (top-level or sub flow) that a sub flow snapshot needs. */
data class FlowSummary(
    val id: UUID,
    val alias: String,
    val providerId: String,
    val description: String? = null
)
