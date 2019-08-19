package dyvilx.tools.gradle

import dyvilx.tools.gradle.internal.DyvilVirtualDirectoryImpl
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.DefaultSourceSetOutput
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet

@CompileStatic
class DyvilPlugin implements Plugin<Project> {
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

		project.tasks.register(taskName, JavaExec, { JavaExec it ->
			it.classpath = it.project.configurations.getByName('dyvilc')
			it.main = 'dyvilx.tools.compiler.Main'

			it.args "source_dirs=${ inputFiles.srcDirs.join(':') }"
			it.args "libraries=${ sourceSet.compileClasspath.join(':') }"
			it.args "output_dir=$outputDirName"
			it.args 'compile', '--ansi', '--machine-markers'

			it.inputs.files inputFiles
			it.outputs.files outputFiles
		} as Action<JavaExec>)

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
		final String outputDir = "$project.buildDir/generated-src/gensrc/$sourceSet.name/$languageName"

		project.tasks.register(taskName, JavaExec, { JavaExec it ->
			it.classpath = it.project.configurations.getByName('gensrc')
			it.main = 'dyvilx.tools.gensrc.Main'

			it.args "source_dirs=${ sourceDirSet.srcDirs.join(':') }"
			it.args "output_dir=$it.temporaryDir/classes"
			it.args "gensrc_dir=$outputDir"
			it.args 'compile', 'test', '--ansi', '--machine-markers' // TODO maybe run using gradle

			it.inputs.files sourceDirSet.srcDirs.collect {
				project.fileTree(it).include('**/*.dgt', '**/*.dgt', '**/*.dgc')
			}
			it.outputs.dir(outputDir)
		} as Action<JavaExec>)

		sourceDirSet.srcDir project.files(outputDir).builtBy(taskName)

		project.tasks.named(sourceSet.getCompileTaskName(languageName)) { Task it ->
			it.dependsOn taskName
		}
	}
}
