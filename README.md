# minecraft-data-generator

This tool generates minecraft-data files based on a running a server on client classpath using a fabric mod.
The supported versions are enumerated in versions.json. 

Every version has its own directory.

## Usage

You can put any version that has a directory into the command below.
Just replace `<version>` with the version you want to generate.

For Linux/Mac OS:

```bash
./gradlew :mc:<version>:runServer
```

For Windows:

```bash
gradlew.bat :mc:<version>:runServer
```

You can then find the minecraft-data in the `mc/<version>/run/minecraft-data` directory.

## Adding a new version

Generally, our automated PR system will automatically create a new PR for a new version, 
so you just have to clone the auto opened PR and continue work on it.

### Manual setup

To add a new version manually, run
```
npm run bump <version>
```

For example, `npm run bump 1.21.6` will:
* Create mc/1.21.6 as a copy of 1.21.5
* Update mc/1.21.6/build.gradle for 1.21.6
* Add the version to `versions.json`

### Updating

Then, you need to fix all code issues that are caused by the new version as `mc/1.21.6` for example may need changes for the latest version of Minecraft.

Refer to Minecraft source code diffs for reference, as the code uses standard Mojmaps.

You can use an IDE like IntelliJ IDEA to manuall fix the issues or an LLM agent that has access to the source code diff.

Once everything compiles, you can commit the changes, push them to your fork and create a pull request.

Once your PR was accepted and merged, the new version will be available in the next release.

## Technical info

By configuring Unimined (the mod build tool used by this project) to run the server, the server will be started with the client classpath.
The Minecraft client always contains a copy of the whole server code.
This is so because the integrated server is always bundled with clients and Mojang has always additionally bundled the dedicated server code within the client code.
This way we can access client information and classes while running a modded server environment.

The `common` module shares common mod logic to allow us to deduplicate similar code across versions.
