# Dyvil Gradle Plugin v0.1.0

+ The plugin now adds `compile*Dyvil` tasks for each source set.
+ The plugin now adds `src/*/dyvil` source directories for each source set.
+ The plugin now adds dependencies on the `compile*Dyvil` tasks to the `*classes` task for each source set.
+ The plugin now adds the `dyvilc` and `gensrc` configurations.

# Dyvil Gradle Plugin v0.2.0

+ The plugin now adds the `compile{,Test}{Java,Dyvil}GenSrc` tasks. #1
+ The plugin now registers GenSrc task output directories as source directories. #1
+ The plugin now registers task dependencies on GenSrc tasks for compile tasks. #1

# Dyvil Gradle Plugin v0.3.0

+ Added custom `Task` subclasses for Dyvil Compile and GenSrc tasks. #2

# Dyvil Gradle Plugin v0.4.0

+ Added a way to convert DyvilCompile/GenSrc tasks to `JavaExec`s.
* The class output directory of GenSrc tasks is now configurable.
* The plugin now checks tool configuration dependencies for minimum required versions.
* Tools are now invoked using the new command-line option style.
* Bumped minimum Dyvil Compiler version to v0.46.3.
* Bumped minimum GenSrc version to v0.10.1.
* Fixed GenSrc tasks not processing `*.dgs` files.
