# The Dyvil Gradle Plugin

[![Build Status](https://travis-ci.org/Dyvil/Dyvil-Gradle-Plugin.svg?branch=master)](https://travis-ci.org/Dyvil/Dyvil-Gradle-Plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/org/dyvil/dyvil-gradle/org.dyvil.dyvil-gradle.gradle.plugin/maven-metadata.xml.svg?colorB=blue&label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/org.dyvil.dyvil-gradle)

## Features

This plugin firmly integrates Dyvil tools into the Gradle ecosystem.
It automatically configures source sets (like `src/main/dyvil` and `src/test/dyvil`),
adds compilation tasks (like `compileDyvil` and `compileTestDyvil`),
and wires everything up to work properly with other languages and build tasks.

In addition to Dyvil compiler support, the plugin allows you to use GenSrc in your project.
It adds tasks that invoke the GenSrc compiler on your Java and Dyvil source directories (`compileJavaGenSrc`, `compileTestJavaGenSrc`, `compileDyvilGenSrc`, and `compileTestDyvilGenSrc`) and ensures they run before any compilation takes place.

## Installation

Add the following snippet to your `build.gradle` to enable the plugin:

```groovy
plugins {
	// ...
	id 'org.dyvil.dyvil-gradle' version '0.2.0'
}
```

Configure the Dyvil compiler and GenSrc versions in the `dependencies` DSL block:

```groovy
dependencies {
	// ...

	// https://mvnrepository.com/artifact/org.dyvil/compiler
	dyvilc group: 'org.dyvil', name: 'compiler', version: 'setme'

	// https://mvnrepository.com/artifact/org.dyvil/gensrc
	gensrc group: 'org.dyvil', name: 'gensrc', version: 'setme'
	
	// ...
}
```

## Examples

See the [Dyvil Property Format repository](https://github.com/Dyvil/Dyvil-Property-Format) for an example on how to use the plugin.
It makes use of both the Dyvil and GenSrc compiler, with minimal build configuration.
