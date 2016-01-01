package dyvil.tools.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class DyvilPlugin implements Plugin<Project>
{
	@Override
	void apply(Project project)
	{
		project.task('hello') >> {
			println("Hello World!")
		}
	}
}
