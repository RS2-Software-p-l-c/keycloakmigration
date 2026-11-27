package de.klg71.keycloakmigration.changeControl.actions.workflow

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.klg71.keycloakmigration.AbstractIntegrationTest
import de.klg71.keycloakmigration.changeControl.actions.Action
import de.klg71.keycloakmigration.changeControl.actions.MigrationException
import de.klg71.keycloakmigration.keycloakapi.KeycloakClient
import de.klg71.keycloakmigration.keycloakapi.model.Workflow
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowConcurrency
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowSchedule
import de.klg71.keycloakmigration.keycloakapi.model.WorkflowStep
import de.klg71.keycloakmigration.keycloakapi.workflowByName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class AddWorkflowIntegTest : AbstractIntegrationTest() {

    private val client by inject<KeycloakClient>()
    private val yamlObjectMapper by inject<ObjectMapper>(named("yamlObjectMapper"))

    private fun workflow(): Workflow? = client.workflowByName(WORKFLOW_NAME, testRealm)

    @Test
    fun testAddWorkflow() {
        addWorkflow(testRealm)

        assertThat(workflow()?.definition()).isEqualTo(
            Workflow(
                name = WORKFLOW_NAME,
                on = "user-created or user-authenticated",
                schedule = WorkflowSchedule(after = "1d", batchSize = 500),
                concurrency = WorkflowConcurrency(restartInProgress = "true"),
                steps = warnThenDisable
            )
        )
    }

    @Test
    fun testAddWorkflowFromChangelogYaml() {
        // `on` is a YAML 1.1 boolean keyword, and restart-in-progress is written as a boolean:
        // both have to survive the changelog's YAML parsing.
        val action = yamlObjectMapper.readValue<Action>(
            """
            addWorkflow:
              realm: $testRealm
              name: $WORKFLOW_NAME
              on: user-created or user-authenticated
              if: has-user-attribute(department)
              schedule:
                after: 1d
                batch-size: 500
              concurrency:
                restart-in-progress: true
              steps:
                - uses: notify-user
                  after: 85d
                  with:
                    subject: accountDisableNotificationSubject
                - uses: grant-role
                  with:
                    role:
                      - first-role
                      - second-role
                - uses: disable-user
                  after: 5d
            """.trimIndent()
        )
        action.executeIt()

        val created = workflow()!!
        assertThat(created.on).isEqualTo("user-created or user-authenticated")
        assertThat(created.condition).isEqualTo("has-user-attribute(department)")
        assertThat(created.schedule).isEqualTo(WorkflowSchedule(after = "1d", batchSize = 500))
        assertThat(created.concurrency).isEqualTo(WorkflowConcurrency(restartInProgress = "true"))
        assertThat(created.steps).containsExactly(
            WorkflowStep("notify-user", "85d", mapOf("subject" to "accountDisableNotificationSubject")),
            WorkflowStep("grant-role", null, mapOf("role" to listOf("first-role", "second-role"))),
            WorkflowStep("disable-user", "5d")
        )
    }

    @Test
    fun testAddWorkflowExisting() {
        addWorkflow(testRealm)

        assertThatThrownBy { addWorkflow(testRealm) }
            .isInstanceOf(MigrationException::class.java)
            .hasMessage("Workflow with name: $WORKFLOW_NAME already exists in realm: $testRealm!")
    }

    @Test
    fun testAddWorkflowReportsKeycloaksReason() {
        assertThatThrownBy { addWorkflow(testRealm, steps = listOf(WorkflowStep(uses = "no-such-step"))) }
            .isInstanceOf(MigrationException::class.java)
            .hasMessageContaining("Status: 400")
            .hasMessageContaining("Could not find step provider: no-such-step")
        assertThat(workflow()).isNull()
    }

    @Test
    fun testUndoAddWorkflow() {
        val action = AddWorkflowAction(realm = testRealm, name = WORKFLOW_NAME, steps = warnThenDisable)
        action.executeIt()
        assertThat(workflow()).isNotNull()

        action.undoIt()

        assertThat(workflow()).isNull()
    }
}
