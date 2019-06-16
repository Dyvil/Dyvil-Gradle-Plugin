package dyvilx.tools.gradle

import dyvilx.tools.gradle.internal.DyvilVirtualDirectoryImpl
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet

@CompileStatic
class DyvilPlugin implements Plugin<Project> {
	@Override
	void apply(Project project) {
		project.pluginManager.apply(JavaPlugin)

		// configurations
		final Configuration dyvilc = project.configurations.create('dyvilc')
		final Configuration gensrc = project.configurations.create('gensrc')

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
		directoryDelegate.dyvil.srcDir(srcDirName)

		new DslObject(sourceSet).convention.plugins.put(DyvilVirtualDirectory.NAME, directoryDelegate)

		sourceSet.allSource.source(directoryDelegate.dyvil)
	}
}
