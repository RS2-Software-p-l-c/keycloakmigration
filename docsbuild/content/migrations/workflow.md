---
author: isaac.mercieca
layout: post
title:  "Workflow Migrations"
date:   2026-11-27 09:00:00 +0200
permalink: /migrations/workflow/
---
# Workflow Migrations
All migrations referring to [workflows](https://www.keycloak.org/docs/latest/server_admin/#_workflows), Keycloak's
automation of administrative tasks such as disabling inactive users. Workflows need Keycloak 26.4 or later.

A workflow is identified by its name, which Keycloak keeps unique within a realm. The parameters are the same as
the keys of a workflow definition in the Keycloak documentation.

A workflow's message can use Keycloak placeholders such as `${user.firstName}`. In a changelog, write these as
`$${user.firstName}`, so the migration does not substitute them itself.

## addWorkflow
Adds a new workflow to an existing realm. Fails if a workflow with the same name already exists.

### Parameters
- realm: String, optional
- name: String, not optional
- on: String, optional, the event expression that starts the workflow, e.g. `user-created or user-authenticated`
- if: String, optional, the condition a resource must meet, e.g. `has-role(employee)`
- schedule: optional
  - after: String, the interval between scheduled runs, e.g. `1d`
  - batch-size: Integer, the maximum number of resources started per run
- concurrency: optional
  - restart-in-progress: Boolean, a new event restarts a running execution from the first step
  - cancel-in-progress: Boolean, a new event cancels a running execution
- enabled: Boolean, optional, default=true
- steps: List, not optional
  - uses: String, not optional, the step, e.g. `notify-user` or `disable-user`
  - after: String, optional, how long to wait after the previous step, e.g. `30d`
  - with: Map< String, Any >, optional, the step's parameters; each value is a string or a list of strings

### Example
```yaml
id: add-inactive-users-workflow
author: isaac.mercieca
changes:
  - addWorkflow:
      name: Disable inactive users
      on: user-created or user-authenticated
      schedule:
        after: 1d
        batch-size: 500
      concurrency:
        restart-in-progress: true
      steps:
        - uses: notify-user
          after: 85d
        - uses: disable-user
          after: 5d
```

### Undo
Deletes the workflow.

## updateWorkflow
Replaces the definition of an existing workflow. This is a full replace, the same as Keycloak's update: a parameter
left out of the change is removed from the workflow, not kept.

While any resource is part-way through the workflow, Keycloak only accepts changes to its settings: `if`,
`schedule`, `concurrency`, `enabled`, and each step's `after` and `with`. It refuses a change to `on`, or to the
number, order or `uses` of the steps, and the migration fails with Keycloak's reason. To make such a change, delete
the workflow and add it again. Every resource then loses its progress and starts from the first step.

### Parameters
The same as [addWorkflow](#addworkflow). `name` selects the workflow to update.

### Example
```yaml
id: run-inactive-users-workflow-twice-a-day
author: isaac.mercieca
changes:
  - updateWorkflow:
      name: Disable inactive users
      on: user-created or user-authenticated
      schedule:
        after: 12h
        batch-size: 500
      concurrency:
        restart-in-progress: true
      steps:
        - uses: notify-user
          after: 85d
        - uses: disable-user
          after: 5d
```

### Undo
Restores the previous definition.

## deleteWorkflow
Deletes a workflow. Keycloak removes every resource's progress through the workflow with it.

### Parameters
- realm: String, optional
- name: String, not optional

### Example
```yaml
id: delete-inactive-users-workflow
author: isaac.mercieca
changes:
  - deleteWorkflow:
      name: Disable inactive users
```

### Undo
Recreates the workflow from its definition. The progress of the resources that were part-way through it cannot be
restored: they start the workflow again from the first step, when its event or schedule next picks them up.
