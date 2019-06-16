package dyvilx.tools.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

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
}
