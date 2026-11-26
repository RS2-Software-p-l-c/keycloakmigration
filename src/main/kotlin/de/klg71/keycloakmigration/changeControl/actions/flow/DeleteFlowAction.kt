package de.klg71.keycloakmigration.changeControl.actions.flow

import de.klg71.keycloakmigration.changeControl.actions.Action
import de.klg71.keycloakmigration.keycloakapi.extractLocationUUID
import de.klg71.keycloakmigration.keycloakapi.flowSnapshot
import de.klg71.keycloakmigration.keycloakapi.model.AddFlow
import de.klg71.keycloakmigration.keycloakapi.model.Flow
import de.klg71.keycloakmigration.keycloakapi.model.FlowNode
import de.klg71.keycloakmigration.keycloakapi.restoreFlowSnapshot

/**
 * Deletes a flow, if one with this alias exists.
 *
 * Undo recreates the flow and rebuilds its executions from a snapshot taken before the delete, sub
 * flows included.
 */
class DeleteFlowAction(
    realm: String?,
    private val alias: String,
) : Action(realm) {

    private var oldFlow: Flow? = null
    private var oldExecutions: List<FlowNode> = emptyList()

    override fun execute() {
        val flow = client.flows(realm()).firstOrNull { it.alias == alias } ?: return
        oldExecutions = client.flowSnapshot(realm(), alias)
        client.deleteFlow(realm(), flow.id)
        oldFlow = flow
    }

    override fun undo() {
        val flow = oldFlow ?: return
        client.addFlow(
            realm(), AddFlow(flow.alias, flow.builtIn, flow.description, flow.providerId, flow.topLevel)
        ).extractLocationUUID()
        client.restoreFlowSnapshot(realm(), flow.alias, oldExecutions)
        oldFlow = null
    }

    override fun name() = "DeleteFlow $alias"
}
