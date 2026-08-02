# UniEnable — ActivityConflictChecker Review and Extraction Plan

> Run this only after the assertions-and-logging batch has been completed, verified, pushed, and the working tree is clean.
>
> Scope: review and, only if justified, extract activity conflict-validation logic from `ActivityManager`.
>
> Do not combine this work with new v2.0 features, date/time parser redesign, storage-format changes, command syntax changes, diagram migration, PDF work, or unrelated cleanup.

## 1. Objective

Determine whether extracting an `ActivityConflictChecker` would materially improve cohesion, readability, and maintainability.

Do not extract the class simply because `ActivityManager` is large.

Proceed only if the proposed class would own a complete and coherent conflict-validation policy rather than merely moving private methods into another file.

The extracted responsibility should cover, where applicable:

- exact duplicate detection;
- scheduling-detail duplicate detection;
- fixed/flexible overlap checks;
- exclusion of the activity currently being edited;
- validation of a batch of activities;
- recurrence conflict validation;
- conflict checks shared by `add`, `edit`, `replace`, and `addAllAtomically`.

# Phase 1 — Read-only design review

Do not modify code during this phase.

## 2. Map the current conflict-validation flow

Inspect at least:

- `ActivityManager`;
- activity add/replace/edit methods;
- `addAllAtomically`;
- recurrence creation flow;
- duplicate and overlap helper methods;
- `FixedActivity`;
- `FlexibleActivity`;
- relevant exception classes;
- all duplicate/overlap tests.

Report:

1. every current conflict-related method;
2. its visibility;
3. its callers;
4. its dependencies;
5. the rules it enforces;
6. whether it mutates state;
7. whether it throws or returns a result;
8. whether the same rule is duplicated elsewhere.

Produce a compact table:

| Current method | Responsibility | Callers | Dependencies | Mutation? | Extraction candidate? |
|---|---|---|---|---|---|

## 3. Confirm the actual policy

Document the current behaviour exactly.

Determine:

- what counts as an exact duplicate;
- what counts as the same scheduling details;
- how fixed/fixed conflicts are detected;
- how fixed/flexible conflicts are detected;
- how flexible/flexible conflicts are detected;
- whether inclusive or exclusive time boundaries are used;
- whether adjacent activities are allowed;
- how an activity being edited is excluded;
- how batch activities are checked against existing activities;
- how batch activities are checked against one another;
- whether recurrence applies any extra rule;
- which exception type and message each conflict produces.

Do not improve or reinterpret the policy during this phase.

## 4. Evaluate cohesion

Assess whether the conflict logic has:

- one clear reason to change;
- a distinct dependency pattern;
- enough internal cohesion to form one class;
- multiple genuine callers;
- a stable API that reduces `ActivityManager` complexity;
- tests that can move cleanly to a dedicated test class.

Give one conclusion only:

- **Keep in `ActivityManager`**
- **Extract `ActivityConflictChecker`**
- **Defer extraction because evidence is insufficient**

Explain the conclusion with concrete evidence.

## 5. Proposed design, if extraction is justified

Show the smallest useful design.

```text
ActivityManager
    owns activities, mutation, permanent IDs, ordering, and queries
    delegates conflict validation

ActivityConflictChecker
    validates one candidate against existing activities
    validates replacement while excluding one activity ID
    validates a batch against existing activities and itself
```

Possible API shape:

```java
final class ActivityConflictChecker {

    void validateForAdd(
            Activity candidate,
            List<Activity> existingActivities)
            throws DuplicateActivityException;

    void validateForReplace(
            Activity candidate,
            int excludedActivityId,
            List<Activity> existingActivities)
            throws DuplicateActivityException;

    void validateBatch(
            List<Activity> candidates,
            List<Activity> existingActivities)
            throws DuplicateActivityException;
}
```

This is illustrative only. Preserve existing exception types and behaviour.

Do not create:

- a generic `ValidationUtils`;
- separate tiny classes for duplicate and overlap checks without a clear reason;
- an interface with only one implementation;
- static global state;
- dependencies on UI, storage, commands, or CLI parsers.

## 6. Stop for approval

Before modifying code, report:

- current policy;
- proposed class responsibilities;
- proposed method signatures;
- files that would change;
- tests that would move;
- compatibility risks;
- recommendation.

Wait for approval before implementation.

# Phase 2 — Implementation, only after approval

## 7. Extraction requirements

If approved:

- create one package-private or appropriately scoped `ActivityConflictChecker`;
- move only conflict-validation logic;
- keep mutation and permanent-ID allocation in `ActivityManager`;
- keep searching, ordering, and `next()` unchanged;
- preserve all current exception types;
- preserve all current exception messages;
- preserve all accepted command behaviour;
- preserve recurrence behaviour;
- preserve storage format;
- preserve public APIs unless explicitly approved.

`ActivityManager` should delegate to the checker rather than duplicate the rules.

## 8. Test migration

Move relevant existing tests rather than copying them.

Create:

```text
ActivityConflictCheckerTest
```

It should own direct tests for:

- exact duplicates;
- scheduling-detail duplicates;
- fixed/fixed overlap;
- fixed/flexible overlap;
- flexible/flexible overlap;
- adjacent non-overlapping activities;
- replacement excluding the original activity;
- replacement conflicting with another activity;
- batch candidate versus existing activities;
- candidate versus candidate within the same batch;
- recurrence-style batch conflicts;
- no-conflict cases.

Keep a small number of integration tests in `ActivityManagerTest` to confirm delegation still works for:

- add;
- replace/edit;
- atomic batch add.

Do not duplicate the full matrix in both classes.

## 9. Boundary cases

Verify explicitly:

- equal start and end boundaries according to current policy;
- one activity ending exactly when another begins;
- flexible window edge cases;
- duration equal to window size;
- excluded ID not found;
- empty existing list;
- empty candidate batch;
- duplicate candidates inside one batch;
- conflicts involving completed activities, if completion status currently matters;
- stable permanent IDs.

Do not change current policy unless a defect is separately demonstrated and approved.

## 10. Javadocs and coding standard

For the new class:

- add concise class-level Javadoc if it helps explain responsibility;
- document non-obvious validation contracts;
- document exclusions and batch behaviour;
- avoid comments that repeat method names;
- follow the supplied NUS Java coding standard;
- use package-private visibility unless broader access is required.

Review:

- naming;
- SLAP;
- nesting;
- duplicated conditions;
- line length;
- boolean names;
- explicit imports.

## 11. Verification

After each logical commit, run:

```text
./gradlew clean check
./gradlew javadoc
```

Run the complete text UI regression.

Also run:

```text
ActivityConflictCheckerTest
ActivityManagerTest
```

Report:

- total tests;
- failures;
- Checkstyle result;
- Javadoc result;
- text UI result;
- whether command syntax changed;
- whether any exception type/message changed;
- whether recurrence behaviour changed.

## 12. Commit guidance

Prefer one focused commit:

```text
refactor: extract activity conflict validation from ActivityManager
```

Use a second commit only if test migration is large enough to review separately:

```text
test: move activity conflict cases to ActivityConflictCheckerTest
```

Do not mix unrelated cleanup into these commits.

## 13. Final report before push

Stop before pushing and provide:

1. commits created;
2. files changed;
3. before/after responsibility summary;
4. final checker API;
5. methods removed from `ActivityManager`;
6. tests moved and tests added;
7. total test count;
8. Checkstyle result;
9. Javadoc result;
10. text UI result;
11. confirmation that user-visible behaviour is unchanged;
12. remaining concerns;
13. push recommendation.

# Acceptance criteria

The extraction is successful only if:

- `ActivityConflictChecker` owns one coherent policy;
- `ActivityManager` becomes easier to read;
- no validation rule is duplicated;
- no new abstraction is speculative;
- existing behaviour is unchanged;
- tests are easier to locate;
- recurrence still validates batches atomically;
- all verification gates pass.

If the review concludes that extraction would merely move private methods without improving the design, keep the current structure and document that decision instead.
