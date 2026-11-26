package de.klg71.keycloakmigration.changeControl.actions.flow

import de.klg71.keycloakmigration.changeControl.actions.Action
import de.klg71.keycloakmigration.keycloakapi.clearFlowExecutions
import de.klg71.keycloakmigration.keycloakapi.flowSnapshot
import de.klg71.keycloakmigration.keycloakapi.model.AuthenticationExecutionImport
import de.klg71.keycloakmigration.keycloakapi.model.Flow
import de.klg71.keycloakmigration.keycloakapi.model.FlowNode
import de.klg71.keycloakmigration.keycloakapi.model.UpdateFlow
import de.klg71.keycloakmigration.keycloakapi.replaceFlowExecutions
import de.klg71.keycloakmigration.keycloakapi.restoreFlowSnapshot

/**
 * Updates a flow in place. Only the given values change: without [executions] the flow's executions
 * are left alone.
 *
 * Undo puts back the flow's alias, description, provider and top-level flag and, if the executions
 * were replaced, rebuilds them from a snapshot taken before the change, sub flows included.
 */
@Suppress("LongParameterList")
class UpdateFlowAction(
    realm: String?,
    private val alias: String,
    private val newAlias: String? = null,
    private val description: String? = null,
    private val providerId: String? = null,
    private val topLevel: Boolean? = null,
    private val executions: List<AuthenticationExecutionImport>? = null
) : Action(realm) {

    private var oldFlow: Flow? = null
    private var oldExecutions: List<FlowNode>? = null

    override fun execute() {
        val flow = client.flows(realm()).firstOrNull { it.alias == alias } ?: return
        oldExecutions = executions?.let { client.flowSnapshot(realm(), alias) }
        oldFlow = flow

        // Replacing the executions takes several calls, so undo has to run if one of them fails
        setExecuted()

        client.updateFlow(
            realm(), flow.id, UpdateFlow(
                flow.id,
                newAlias ?: flow.alias,
                providerId ?: flow.providerId,
                topLevel ?: flow.topLevel,
                description ?: flow.description
            )
        )
        executions?.let { client.replaceFlowExecutions(realm(), newAlias ?: flow.alias, it) }
    }

    override fun undo() {
        val flow = oldFlow ?: return
        // Look the flow up by id: the rename may or may not have happened if execute() failed
        if (client.flows(realm()).none { it.id == flow.id }) return

        client.updateFlow(
            realm(), flow.id, UpdateFlow(flow.id, flow.alias, flow.providerId, flow.topLevel, flow.description)
        )
        oldExecutions?.let {
            client.clearFlowExecutions(realm(), flow.alias)
            client.restoreFlowSnapshot(realm(), flow.alias, it)
        }
    }

    override fun name() = "UpdateFlow $alias"
}
