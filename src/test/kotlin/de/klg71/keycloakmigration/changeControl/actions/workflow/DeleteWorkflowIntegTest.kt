package de.klg71.keycloakmigration.changeControl.actions.workflow

import de.klg71.keycloakmigration.AbstractIntegrationTest
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.KeycloakClient
import de.klg71.keycloakmigration.keycloakapi.workflowByName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.koin.core.component.inject

class DeleteWorkflowIntegTest : AbstractIntegrationTest() {

    private val client by inject<KeycloakClient>()

    @Test
    fun testDeleteWorkflow() {
        addWorkflow(testRealm)

        DeleteWorkflowAction(testRealm, WORKFLOW_NAME).executeIt()

        assertThat(client.workflowByName(WORKFLOW_NAME, testRealm)).isNull()
    }

    @Test
    fun testDeleteMissingWorkflow() {
        assertThatThrownBy { DeleteWorkflowAction(testRealm, WORKFLOW_NAME).executeIt() }
            .isInstanceOf(MigrationException::class.java)
            .hasMessage("Workflow with name: $WORKFLOW_NAME does not exist in realm: $testRealm!")
    }

    @Test
    fun testUndoDeleteWorkflow() {
        addWorkflow(testRealm)
        val before = client.workflowByName(WORKFLOW_NAME, testRealm)!!.definition()
        val action = DeleteWorkflowAction(testRealm, WORKFLOW_NAME)
        action.executeIt()

        action.undoIt()

        assertThat(client.workflowByName(WORKFLOW_NAME, testRealm)?.definition()).isEqualTo(before)
    }
}
