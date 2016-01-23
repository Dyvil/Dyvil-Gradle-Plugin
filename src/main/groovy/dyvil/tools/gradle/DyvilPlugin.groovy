package dyvil.tools.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class DyvilPlugin implements Plugin<Project>
{
	@Override
	void apply(Project project)
	{
		Task task = project.task('dyvil')
		task.doLast {
			print("Hello Dyvil!")
		}
	}
}
