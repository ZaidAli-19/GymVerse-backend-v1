---
name: project-context-sync
description: >-
  Synchronizes, reads, and automatically maintains the GymVerse project context and execution state stored in antigravity/project-context.md.
  Use when starting a session, resuming work, asking "where did we leave off?", "what is the current stage of the project?", "what is next?",
  or after implementing/verifying a story or architectural change to keep the context file continuously updated.
---

# Project Context Sync & Maintenance Skill

This skill allows the agent to immediately load project context, verify runtime state against git/filesystem truth, and automatically maintain `antigravity/project-context.md` across development sessions.

---

## 1. Context Retrieval Workflow (When Starting or Resuming)

When invoked to retrieve context or answer "where did we leave off?" / "what is the current stage?":

1. **Read the Primary Context File**:
   - Inspect [`antigravity/project-context.md`](file:///D:/projects/GymVerse-Backend-v2/antigravity/project-context.md).
   - Extract:
     - Current Phase & Active Stage.
     - Last Completed Story and commit reference.
     - Immediate Next Story and its requirements.
     - Test Suite pass count and protected code status.

2. **Verify Live Repository State**:
   - Run isolated git status/log check to confirm the working directory state:
     ```powershell
     $env:GIT_CONFIG_GLOBAL='NUL'; $env:GIT_CONFIG_SYSTEM='NUL'; git status; git log -n 3 --oneline
     ```
   - Check if there are unstaged changes or uncommitted work in progress.

3. **Check Story Planning Artifacts**:
   - Check `_bmad-output/planning-artifacts/` for the current phase (e.g. `phase-2-platform-admin/`) to verify acceptance criteria and test requirements for the active or next story.

4. **Present Status to User**:
   - Provide a concise summary:
     - **Current Stage**: Active phase and exact milestone.
     - **Last Completed Work**: What was finished and verified in the previous session.
     - **Immediate Next Action**: The exact story or task ready to be executed.
     - **Architectural Reminders**: Single controller gateway, factory pattern, protected multitenancy classes, and no-autonomous-commit policy.

---

## 2. Automatic Context Maintenance Workflow (When Completing Work)

Whenever you complete a story, bugfix, refactoring, or planning milestone, **you must automatically update the context file** before concluding your response:

1. **Update Quick Status Card (`antigravity/project-context.md` Section 1)**:
   - Update `Last Completed Story` to the story just finished.
   - Update `Current Phase Stage` if stage transitioned.
   - Update `Immediate Next Story` to point to the next task in the queue.
   - Update `Test Suite Health` with the latest passing test count.

2. **Update Phase Story Breakdown Table (Section 4)**:
   - Mark the completed story with `✅ Completed` and summarize the key artifacts created or modified.

3. **Update Next Work Details (Section 5)**:
   - Replace or update Section 5 with the detailed scope, target files, and acceptance criteria for the upcoming story so the next turn/session has immediate focus.

4. **Verify Mandatory Engineering Directives**:
   - Ensure a dedicated task log was created in `antigravity/<task-name>.md`.
   - Ensure `docs/payloads.md` was updated if any API contracts, DTOs, or cURL requests were affected.
   - Confirm that all 8 protected multitenancy classes in `com.company.GymVerse.multitanancy` remain 100% byte-for-byte untouched.
   - Confirm **NO autonomous `git commit` or `git push`** was run (`RULES.md` Rule 1).

5. **Notify the User**:
   - Inform the user that `antigravity/project-context.md` has been updated with the latest progress and next steps.
