minecraft-data-generator extracts and generates Minecraft server-side data for multiple Minecraft versions via a Fabric mod that hooks into the vanilla Minecraft server.
This data that is generated is then stored inside the https://github.com/PrismarineJS/minecraft-data repo, a cross language repo holding minecraft data on blocks, items and more.

There is a different mod for each Minecraft version this repo supports, under the `mc/<version>` folder.
Each version has a Gradle project at `:mc:<version>` and generating the data is done by running the Gradle task:
```
./gradlew :mc:<version>:runServer
```
Generated data ends up under `mc/<version>/run/server/minecraft-data` in the repo when run locally.

## Repo Layout
- mc/                       — per-Minecraft-version directories of OUR extractor code (e.g. `mc/1.21.8`)
- versions.json             — canonical JSON array of versions that are supported (i.e. there are folders in mc/ for it)
- mc-source/<version>       — (External) minecraft java edition code for relevant version with Mojang mappings that you can reference for changes.
    * we store the last 2 most recent versions in the folder to save space, but if you need more versions you can do `git fetch origin client1.21.3 && git switch client1.21.3`
    * there is a .diff file in mc-source/ like `1.21.7_to_1.21.8.diff` that contains result of `git diff --no-index old new` that you can reference for changes (note it's typically large)
    * Since our generator is using Mojang mappings, the API naming is the same.
- README.md — info how to set up

## Typical Flow

minecraft-data-generator typically needs updates every time there is a new Minecraft version, particularly if there was code changes
around one of the areas that our extractors touches (e.g. an API changes, gets renamed, etc.).

As explained in the README.md, there is an auto update workflow that typically creates scaffolding PRs (under the `bump` branch) whenever there is a new minecraft update.
These PRs are simply copying old version code into new, so there may be changes and other code fixes needed to support the new version. It's your
job to help plug the gaps by making these changes until the build passes for the given version.
It's also possible that there maybe missing or broken generated data even if the build is passing: in these cases, listen to the user's request
and figure out how to replicate, debug and fix the issues in data generation that may be a logical issue over a syntax one.

## Troubleshooting

Simply iterating over errors one by one is a good way to fix most issues. Inside mc-source/ you are provided the latest
code for the latest minecraft versions you can reference to investigate any code changes that we may need to accomodate in our
generator code.

Sometimes, some APIs might change inside the mc code and make the data that the current code extracts no longer
valid. In these cases, do your best to conform the new data to the old structure even if it feels wrong. If you
can't, for example, let's say something about effects are removed from the game. But our data gen still expects it.
What to do? You should set the data to null, and inform the user about the issue -- perhaps a schema change is needed.
You can propose a new schema to the user (so that the relevant changes can be made to minecraft-data) and if
user confirms, you'd update the generator to output in that new schema.

### Steps
- Reproduce the problem locally inside the container and run the generator task.
  - Example run command: `./gradlew :mc:1.21.6:runServer --stacktrace`
  - See if the problem exists on an older version -- if so inform the user, otherwise compare logs and code
- Inspect diffs in `mc-source/` to correlate Mojang API changes.
  - See [mc-source/](mc-source/)
- Validate generated output at `mc/<version>/run/server/minecraft-data` to make sure all files are there

---

## Examples

Session 1 — Compile error after bump PR
- User prompt: "The bump PR for mc/1.21.8 fails to compile. Build error: cannot find symbol method RegistryKey.of(...)."
- Agent actions:
  1. Reproduce: `./gradlew :mc:1.21.8:runServer --stacktrace`
  2. Inspect compile error and mc-source/1.21.7_to_1.21.8.diff for API renames.
  3. Update generator code: edit mc/1.21.8/src/main/java/.../SomeRegistryAdapter.java to use the new Registry API.
  4. Build again and run generation.

Session 2 — Runtime NPE during generation
- User prompt: "Generation starts but crashes with NullPointerException in EnchantmentsDataGenerator."
- Agent actions:
  1. Reproduce with stacktrace: `./gradlew :mc:1.20.4:runServer --stacktrace`
  2. Add temporary logging to EnchantmentsDataGenerator to identify the null object, or run in debugger.
  3. Fix by guarding against null Registries or empty lists and add defensive checks.
  4. Re-run generation and verify produced files.
- If problem persists:
  * Add logging to old version too
  * Check relevant code diff to see if anything changed related to this

Session 3 — Semantically incorrect generated data
- User prompt: "Output JSON is valid but item tags are incorrect after the bump."
- Agent actions:
  1. Reproduce generation and open the produced JSON at mc/<version>/run/server/minecraft-data/tags/items.json
  2. Compare against previous version output (`git diff --no-index`) to identify mismatches.
  3. Inspect generator assumptions vs. mc-source diffs (names, tag keys, or logic changes).
  4. Correct mapping logic in generator class and run generation.
