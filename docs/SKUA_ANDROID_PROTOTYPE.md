# Skua-style Android prototype — phase 1

This branch is isolated from the working farm/boss branch.

## 1. Executor
`ScriptRuntime` interprets a deliberately limited, local command language: `status`, `targets`, `skills`, and `echo text`. It enforces a 100-command limit and exposes stop(). No arbitrary Kotlin, C#, JavaScript or network execution.

## 2. UI
The existing Android application already has an overlay bubble (`BubbleService`) and combat configuration screen. These are the starting point for a future script-editor tab. This commit does **not** claim to add an editor or hook the runtime into the overlay.

## 3. Game communication
`GameStateBridge` is a read-only interface, with `DemoGameStateBridge` as a simulator. It does not read the official client, inspect game memory, intercept traffic, or execute game actions. A real integration requires a supported, authorized interface.

## Next checks
- Add an in-app editor with execute/stop buttons and logs.
- Unit-test parsing, command limits, cancellation, and UI lifecycle.
- Verify Android CI builds before offering an APK.
- Only connect to real game state if an authorized integration exists.

Example script:
```
status
targets
skills
echo Olá, AQW!
```
