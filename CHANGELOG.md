# Dyvil Gradle Plugin v0.1.0

+ The plugin now adds `compile*Dyvil` tasks for each source set.
+ The plugin now adds `src/*/dyvil` source directories for each source set.
+ The plugin now adds dependencies on the `compile*Dyvil` tasks to the `*classes` task for each source set.
+ The plugin now adds the `dyvilc` and `gensrc` configurations.

# Dyvil Gradle Plugin v0.2.0

+ The plugin now adds the `compile{,Test}{Java,Dyvil}GenSrc` tasks. #1
+ The plugin now registers GenSrc task output directories as source directories. #1
+ The plugin now registers task dependencies on GenSrc tasks for compile tasks. #1
