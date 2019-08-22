package dyvilx.tools.gradle

import dyvilx.tools.gradle.internal.DyvilVirtualDirectoryImpl
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.DefaultSourceSetOutput
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

@CompileStatic
class DyvilPlugin implements Plugin<Project> {

	public static final String DYVILC_MAIN = 'dyvilx.tools.compiler.Main'
	public static final String GENSRC_MAIN = 'dyvilx.tools.gensrc.Main'

	@Override
	void apply(Project project) {
		project.pluginManager.apply(JavaPlugin)

		// configurations
		project.configurations.register('dyvilc')
		project.configurations.register('gensrc')

		project.convention.getPlugin(JavaPluginConvention).sourceSets.each {
			configureSourceSet(project, it)
		}
	}

	static void configureSourceSet(Project project, SourceSet sourceSet) {
		// for each source set we will:
		// 1) create a new virtual directory mapping

		final String srcDirName = "src/$sourceSet.name/dyvil"
		final File srcDir = project.file(srcDirName)

		final DyvilVirtualDirectoryImpl directoryDelegate = new DyvilVirtualDirectoryImpl(sourceSet, project.objects)
		final SourceDirectorySet inputFiles = directoryDelegate.dyvil
		inputFiles.srcDir(srcDirName)

		new DslObject(sourceSet).convention.plugins.put(DyvilVirtualDirectory.NAME, directoryDelegate)

		sourceSet.allSource.source(inputFiles)

		// 2) create a dyvil compile task

		final String taskName = sourceSet.getCompileTaskName("dyvil")
		final String outputDirName = "$project.buildDir/classes/dyvil/$sourceSet.name/"
		final File outputDir = project.file(outputDirName)
		final ConfigurableFileTree outputFiles = project.fileTree(outputDir)

		inputFiles.setOutputDir(outputDir)
		outputFiles.include("**/*.class", "**/*.dyo")
		outputFiles.builtBy taskName

		((DefaultSourceSetOutput) sourceSet.output).addClassesDir { outputDir }

		project.tasks.register(taskName, DyvilCompileTask, { DyvilCompileTask it ->
			it.description = "Compiles the $sourceSet.name Dyvil code."

			// 3) set up convention mapping for default sources (allows user to not have to specify)
			it.classpath = sourceSet.compileClasspath
			it.dyvilcClasspath = project.configurations.getByName('dyvilc')
			it.destinationDir = outputDir
			it.source = directoryDelegate.dyvil
		} as Action<DyvilCompileTask>)

		// 3) make the classes task depend on our compile task
		project.tasks.named(sourceSet.classesTaskName) { Task it ->
			it.dependsOn taskName
		}

		// 4) configure gensrc for each source directory set (only java and dyvil, for now)
		configureGenSrc(project, sourceSet, sourceSet.java)
		configureGenSrc(project, sourceSet, inputFiles)
	}

	static void configureGenSrc(Project project, SourceSet sourceSet, SourceDirectorySet sourceDirSet) {
		final String languageName = sourceDirSet.name
		final String taskName = sourceSet.getCompileTaskName("${ languageName }GenSrc")
		final File outputDir = project.file("$project.buildDir/generated-src/gensrc/$sourceSet.name/$languageName")

		project.tasks.register(taskName, GenSrcTask, { GenSrcTask it ->
			it.description = "Processes the $sourceSet.name GenSrc files."

			final Configuration gensrc = project.configurations.getByName('gensrc')
			it.classpath = gensrc
			it.dyvilcClasspath = gensrc

			it.destinationDir = outputDir
			it.source = sourceDirSet
		} as Action<GenSrcTask>)

		sourceDirSet.srcDir project.files(outputDir).builtBy(taskName)

		project.tasks.named(sourceSet.getCompileTaskName(languageName)) { Task it ->
			it.dependsOn taskName
		}
	}
}
