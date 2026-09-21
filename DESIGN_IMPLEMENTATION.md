# GhostFrame: implement the design system

## Goal

Apply the supplied GhostFrame design system to the existing Kotlin Android app. Work directly in this repository and explain changes briefly for a developer learning Android. Implement this first styling phase only.

## Design references

The user will place these references in `docs/design/`:

- `Design.html`: source for exact design values; includes five screens and a Design system v1.0 board.
- Preview & replace screenshot.
- Pinned (idle) screenshot.
- Pinned tools-open screenshot.

Read the HTML and inspect the screenshots before implementing. The HTML is a self-contained bundle: its `__bundler/template` and `__bundler/manifest` script elements contain JSON, with some base64/gzip assets and nested page bundles. Extract design content if necessary; do not mistake the outer loading page for the design.

Use the Design system v1.0 board for shared tokens and the individual screens for component appearance. Do not copy the mock reference-photo skeletons or simulated underlying app into the actual app UI.

If references are missing, identify the missing files before inventing replacement designs.

## Inspect the project first

- Read applicable AGENTS.md instructions and inspect Git status. Preserve existing user changes.
- Inspect actual Gradle configuration and dependency versions. Do not infer the project Kotlin version from Gradle's own embedded Kotlin version.
- Locate current theme, home UI, overlay service, controller, window host, menu, photo view, and opacity view.
- Keep the existing Compose home UI and Android View overlay approach for this phase.
- Avoid dependency upgrades, architecture rewrites, and new libraries unless necessary and explained.

## Design tokens

Create one authoritative palette that both Compose and Android Views can consume. Prefer existing project conventions and simple adapters; avoid duplicate hex values throughout components.

| Role | Value |
| --- | --- |
| Canvas | #0E0F12 |
| Surface 1 | #16181D |
| Surface 2 | #1F2229 |
| Border | #2A2E37 |
| Primary text | #F2F3F5 |
| Secondary text | #A7ACB8 |
| Mint accent | #7CF0C4 |
| Amber | #F5B841 |

Use mint for primary actions, selected controls, and the frame outline. Use dark text/icons on mint controls. Reserve amber for Unpin-related styling. Avoid mint body copy.

Typography from the design board:

| Role | Font | Weight | Size / line height |
| --- | --- | --- | --- |
| Display | Space Grotesk | 700 | 40 / 42 |
| Title | Space Grotesk | 600 | 22 / 28 |
| Button | Space Grotesk | 600 | 17 / 24 |
| Body | IBM Plex Sans | 400 | 16 / 24 |
| Caption | IBM Plex Sans | 400 | 13 / 18 |
| Overline | IBM Plex Sans | 500 | 11 / 16 |
| Readout | JetBrains Mono | 400 | 14 / 20 |

Use Android sp for text and dp for layout. Preserve the design's 4-unit spacing rhythm. Inspect component radii and dimensions in the HTML rather than guessing a single radius for everything. Support font scaling and use at least 48dp interactive touch targets.

Use properly licensed, Android-compatible bundled font files if available. Do not pass WOFF2 files to Android font resources. If the supplied font assets cannot be used, report the limitation and use explicit temporary fallbacks; do not claim exact typography.

## Implementation scope: first styling phase

1. Define shared colors, spacing, shapes, and typography using existing project structure where practical.
2. Connect these to GhostFrameTheme. Ensure system dynamic colors do not override the supplied palette. The supplied design is dark; avoid inventing a light theme in this phase.
3. Restyle the existing home UI's background, text, buttons, and existing photo preview, if present. Keep current photo selection, permission flow, and overlay launching behavior.
4. Restyle the existing floating menu button and opacity panel: dark surfaces, subtle borders, mint slider track/thumb, readable values, and clear pressed/selected states. Keep the current menu interaction for this phase; do not build the radial tool menu yet.
5. Keep an explicit Done action on the opacity panel. Preserve separate meanings for dismissing controls and stopping the overlay. If the current Close action stops the overlay, label it Unpin; a tools-dismiss X must never stop the service.
6. Use accessible labels for icon-only buttons and keep controls reachable within screen/system-bar insets.

Do not add nonfunctional Crop, Rotate, or Swap buttons merely to match the screenshot. Full preview navigation, the radial menu, status badge, selection outline/handles, crop, rotation, and overlay photo replacement are later phases.

## Preserve overlay behavior

- Only the photo window changes opacity. Menu and slider controls remain fully opaque and tappable.
- Preserve the current 0–80% opacity range and fine slider steps.
- Preserve touch-through outside control windows when repositioning is off.
- Avoid a full-screen touchable container for a small control panel.
- Preserve foreground service notification, permission checks, asynchronous image loading, cancellation, and window cleanup.
- Preserve the existing optimization that only changes touch flags when the mode actually changes.
- Do not repeatedly reset the slider thumb during user dragging.

Known issue: repositioning can stop responding after using opacity controls. Investigation was explicitly deferred. Record it as an existing issue, do not silently expand this styling task into debugging it, and do not claim it is fixed.

## Validation and delivery

Automated tests are deferred by the user; do not add a test framework or a new test suite for this task.

- Run `./gradlew assembleDebug` if the local Android environment is available.
- If an emulator/device is available, perform a focused visual check of the home UI and opacity controls. If not, give short manual verification steps and state that visual validation remains pending.
- Do not claim build, installation, or device verification unless actually performed.
- Do not publish a release, push commits, or change app version numbers as part of this task.
- Keep changes reviewable in small logical groups. Offer concise suggested commit messages without creating commits unless requested.

Finish with a short explanation of what changed, which shared tokens feed Compose and Android Views, build results, any font or styling limitations, and the next small design implementation step.
