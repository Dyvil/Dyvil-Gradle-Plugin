package dyvilx.tools.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static org.hamcrest.CoreMatchers.hasItem
import static org.hamcrest.CoreMatchers.notNullValue
import static org.junit.Assert.assertThat

class DyvilPluginTest {
	private Project project

	@Before
	void setup() {
		this.project = ProjectBuilder.builder().withName('test').build()
		this.project.pluginManager.apply 'org.dyvil.dyvil-gradle'
	}

	@Test
	void addsConfigurations() {
		assertThat(project.configurations.dyvilc, notNullValue())
		assertThat(project.configurations.gensrc, notNullValue())
	}

	@Test
	void addsSourceSets() {
		assertThat(project.sourceSets.main.dyvil, notNullValue())
		assertThat(project.sourceSets.test.dyvil, notNullValue())
	}

	@Test
	void addsCompileTasks() {
		assertThat(project.tasks.compileDyvil, notNullValue())
		assertThat(project.tasks.compileTestDyvil, notNullValue())
	}

	@Test
	void addsGenSrcTasks() {
		assertThat(project.tasks.compileJavaGenSrc, notNullValue())
		assertThat(project.tasks.compileTestJavaGenSrc, notNullValue())
		assertThat(project.tasks.compileDyvilGenSrc, notNullValue())
		assertThat(project.tasks.compileTestDyvilGenSrc, notNullValue())
		assertThat(project.tasks.generateJavaGenSrc, notNullValue())
		assertThat(project.tasks.generateTestJavaGenSrc, notNullValue())
		assertThat(project.tasks.generateDyvilGenSrc, notNullValue())
		assertThat(project.tasks.generateTestDyvilGenSrc, notNullValue())
	}

	@Test
	void addsTaskDependencies() {
		assertThat(project.tasks.classes.dependsOn, hasItem('compileDyvil'))
		assertThat(project.tasks.testClasses.dependsOn, hasItem('compileTestDyvil'))

		assertThat(project.tasks.compileJava.dependsOn, hasItem('generateJavaGenSrc'))
		assertThat(project.tasks.compileTestJava.dependsOn, hasItem('generateTestJavaGenSrc'))
		assertThat(project.tasks.compileDyvil.dependsOn, hasItem('generateDyvilGenSrc'))
		assertThat(project.tasks.compileTestDyvil.dependsOn, hasItem('generateTestDyvilGenSrc'))
	}
}
