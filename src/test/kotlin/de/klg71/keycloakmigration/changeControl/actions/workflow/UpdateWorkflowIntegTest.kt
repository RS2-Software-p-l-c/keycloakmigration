package de.klg71.keycloakmigration.changeControl.actions.workflow

import de.klg71.keycloakmigration.AbstractIntegrationTest
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.changeControl.actions.user.AddUserAction
import de.klg71.keycloakmigration.keycloakapi.KeycloakClient
import de.klg71.keycloakmigration.keycloakapi.model.Workflow
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowConcurrency
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowSchedule
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowStep
import de.klg71.keycloakmigration.keycloakapi.userByName
import de.klg71.keycloakmigration.keycloakapi.workflowByName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.koin.core.component.inject

private const val SCHEDULE_TIMEOUT_MILLIS = 10_000L
private const val POLL_MILLIS = 200L

class UpdateWorkflowIntegTest : AbstractIntegrationTest() {

    private val client by inject<KeycloakClient>()

    private fun workflow(): Workflow = client.workflowByName(WORKFLOW_NAME, testRealm)!!

    private fun update(
        on: String? = "user-created or user-authenticated",
        schedule: WorkflowSchedule? = WorkflowSchedule(after = "1d", batchSize = 500),
        steps: List<WorkflowStep> = warnThenDisable
    ) = UpdateWorkflowAction(
        realm = testRealm,
        name = WORKFLOW_NAME,
        on = on,
        schedule = schedule,
        concurrency = WorkflowConcurrency(restartInProgress = "true"),
        steps = steps
    )

    /** A user created after the workflow starts it, so Keycloak holds a scheduled step for them. */
    private fun startWorkflowForAUser() {
        AddUserAction(testRealm, "workflow-user").executeIt()
        val userId = client.userByName("workflow-user", testRealm).id.toString()
        // Keycloak schedules the execution asynchronously, after the user-created event.
        val deadline = System.currentTimeMillis() + SCHEDULE_TIMEOUT_MILLIS
        while (client.workflowsScheduledFor(testRealm, userId).none { it.name == WORKFLOW_NAME }) {
            check(System.currentTimeMillis() < deadline) { "$WORKFLOW_NAME was never scheduled for the user" }
            Thread.sleep(POLL_MILLIS)
        }
    }

    @Test
    fun testUpdateWorkflowWithoutResourcesInProgress() {
        addWorkflow(testRealm)
        val steps = listOf(WorkflowStep(uses = "disable-user", after = "90d"))

        update(on = "user-authenticated", steps = steps).executeIt()

        assertThat(workflow().on).isEqualTo("user-authenticated")
        assertThat(workflow().steps).isEqualTo(steps)
    }

    @Test
    fun testUpdateIsAFullReplace() {
        addWorkflow(testRealm)

        update(schedule = null).executeIt()

        assertThat(workflow().schedule).isNull()
    }

    @Test
    fun testUpdateSettingsWithResourcesInProgress() {
        addWorkflow(testRealm, on = "user-created")
        startWorkflowForAUser()
        val steps = listOf(
            WorkflowStep(uses = "notify-user", after = "80d", with = mapOf("subject" to "otherSubject")),
            WorkflowStep(uses = "disable-user", after = "10d")
        )

        update(on = "user-created", schedule = WorkflowSchedule(after = "12h"), steps = steps).executeIt()

        assertThat(workflow().schedule).isEqualTo(WorkflowSchedule(after = "12h"))
        assertThat(workflow().steps).isEqualTo(steps)
    }

    @Test
    fun testUpdateStepsWithResourcesInProgressFails() {
        addWorkflow(testRealm, on = "user-created")
        startWorkflowForAUser()
        val before = workflow()

        assertThatThrownBy {
            update(on = "user-created", steps = listOf(WorkflowStep(uses = "disable-user", after = "90d"))).executeIt()
        }
            .isInstanceOf(MigrationException::class.java)
            .hasMessageContaining("Status: 400")
            .hasMessageContaining("Use deleteWorkflow followed by addWorkflow")

        assertThat(workflow()).isEqualTo(before)
    }

    @Test
    fun testUpdateMissingWorkflow() {
        assertThatThrownBy { update().executeIt() }
            .isInstanceOf(MigrationException::class.java)
            .hasMessage("Workflow with name: $WORKFLOW_NAME does not exist in realm: $testRealm!")
    }

    @Test
    fun testUndoUpdateWorkflow() {
        addWorkflow(testRealm)
        val before = workflow().definition()
        val action = update(on = "user-authenticated", steps = listOf(WorkflowStep(uses = "disable-user")))
        action.executeIt()

        action.undoIt()

        assertThat(workflow().definition()).isEqualTo(before)
    }
}
