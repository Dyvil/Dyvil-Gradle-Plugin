package dyvilx.tools.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

@CompileStatic
class DyvilPlugin implements Plugin<Project> {
	@Override
	void apply(Project project) {
		// configurations
		final Configuration dyvilc = project.configurations.create('dyvilc')
		final Configuration gensrc = project.configurations.create('gensrc')
	}
}
