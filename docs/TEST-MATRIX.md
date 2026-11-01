# Test matrix

| Check | Who | Procedure | Expected result | Status |
|---|---|---|---|---|
| Java/launcher baseline | Codex | Inspect local Java and launcher profile | JDK/JRE 8 and Forge 1.7.10 profile | Found Java 8 and profile `ForgeOptiFine 1.7.10`; exact Forge build pending |
| HBM baseline | Codex | Inspect local mods folder | Record exact HBM jar | Found `HBM-NTM-1.0.27_X5687.jar`; not loaded/tested |
| Compile | Codex | Build with pinned Forge MDK/Gradle | Clean compile | Pending project bootstrap |
| Dedicated inventory | Codex | Unit/integration test slot serialization and bounds | Slot persists; invalid indices rejected | Pending implementation |
| Server authority | Codex | Multiplayer test swap packet with invalid slot/item data | Server rejects invalid requests; valid state syncs | Manual server test required |
| Swap key | User | Load in Forge 1.7.10, change key binding, swap with empty/occupied offhand | Items exchange once and persist after relog | User test required |
| Item rendering | User | Test first and third person, GUI open, invisible/armor states | Correct item and transform; no missing texture/crash | User test required |
| Food use | User | Hold food in offhand and activate use | One consumption, correct hunger/saturation | User test required; support not yet claimed |
| Torch placement | User | Try offhand torch on valid/invalid surfaces | Correct placement, no duplicate block/item | User test required; support not yet claimed |
| HBM interoperability | User | Test representative HBM tools/items in both hands and reload world | No crash, duplication, desync, or unintended machine action | User test required; especially important |
| Other mods | User | Enable full modpack; inspect startup and interaction logs | No registry, packet, or rendering conflicts | User test required |
