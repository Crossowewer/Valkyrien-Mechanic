<h1 align="center">
Valkyrien Skies : Addon Template
</h1>
<p align="center">
The official VS Addon template, for VS <b>2.4.0</b> and onward!
</p>

In this mod's `gradle.properties` file, you will find a few properties that define the versions of
each dependency this project has, including VS2 and its core. You can change these properties to update
dependency versions easily across the entire project, without having to edit buildscripts!

Most dependencies have some easy way to discover the latest/recommended version to depend on.
Valkyrien Skies itself is similar! The `vs_core_version` property should be equal to the identically named property in
the [Valkyrien Skies Github Repo](https://github.com/ValkyrienSkies/Valkyrien-Skies-2),
and the `vs2_version` is the mod version, followed by the <b>first 10 characters</b> of the latest commit hash.

You can find the most recent
retrievable version of VS easily by going to the Packages tab
for [Fabric](https://github.com/ValkyrienSkies/Valkyrien-Skies-2/packages/2020982)
or [Forge](https://github.com/ValkyrienSkies/Valkyrien-Skies-2/packages/2020984)!

Before running the mod after each change made, it is recommended to run the `./gradlew clean` task in your command line,
followed by clicking the sync button in the top right.
This ensures the files built are up to date with your code changes.

Alternatively to gradle clean, you can also manually delete your build folders. This is helpful if that task fails, for
whatever reason.

To run the mod, run the `mod development/runClient` gradle task.

To get an exported jar, run the `build/build` gradle task, then find your jar at `<root>/build/libs`

- `src/main/java` : This is where Java code goes. This is primarily used for Mixins, as Mixins cannot be written in
  Kotlin.
- `src/main/kotlin` : This is where Kotlin code goes. VS is primarily written in Kotlin, so it tends to be easier to
  work with VS when also writing in Kotlin.
- `src/main/resources` : This is where resources go, such as Forge's `mods.toml`, block models, item textures, etc

You can access the VS Core Api statically through `ValkyrienSkies.api` (with parenthesis for a method call in Java). The
API's javadocs contain tons of extra information on how to use each part of it, so give them a read!

Additionally, there are a few features in VS2 itself- such as `BlockEntityPhysicsListener`- that you may want to use.

<i>This template was built for Valkyrien Skies 2.4.0+. Changes in API from 2.3 will make it almost certainly not function with releases of the mod prior to 2.4, so make sure you intend to develop for that version!</i>