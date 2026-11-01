# Offhand 1710

An experimental Forge 1.7.10 mod that adds a synchronized second-hand item slot.

## Current status

This repository is at the bootstrap/specification stage. The installed game has Java 8, a Forge 1.7.10 launcher profile, and HBM-NTM `1.0.27_X5687`. No source project or Git repository was present initially. Runtime integration with HBM has not been verified.

## Initial scope

- A dedicated offhand inventory slot, separate from the hotbar.
- A configurable key to swap the selected hotbar item with the offhand slot.
- Server-authoritative slot changes and packet synchronization in multiplayer.
- Render the item in first and third person.
- A conservative use action for items explicitly marked as offhand-compatible.
- Compatibility checks for food, torches, shields, and HBM items before claiming support.

This is a mod for Forge 1.7.10, not a backport of Minecraft's 1.12.2 engine. Legacy mod APIs differ significantly; unsupported actions must fail safely rather than run twice on client and server.

## Build environment

Use the Forge 1.7.10 MDK and ForgeGradle 1.2 with JDK 8. The exact Forge build will be pinned after confirming the installed profile's Forge version. A normal build is `gradlew build`; output should be under `build/libs/`.

## Installation

Copy the built jar into the `mods` folder of a Forge 1.7.10 instance. Back up worlds before testing experimental builds. Do not install this jar into a different Minecraft version.

## Testing

See [`docs/TEST-MATRIX.md`](docs/TEST-MATRIX.md) for automated and in-game checks. HBM compatibility is not claimed until tested against the listed installed version.
