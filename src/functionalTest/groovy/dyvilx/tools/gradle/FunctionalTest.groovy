package dyvilx.tools.gradle

import groovy.io.FileType
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@CompileStatic
class FunctionalTest extends Specification {
	static final String TEST_FILES_ROOT = 'src/functionalTest/resources'
	static final String[] TEST_FILES = [
			'build.gradle',
			'settings.gradle',
			'src/main/dyvil/org/example/Foo.dyv',
			'src/main/dyvil/org/example/Bar.dyv.dgt',
			'src/main/java/org/example/Jav.java.dgt',
			'src/test/dyvil/org/example/FooTest.dyv',
	]

	@Rule
	TemporaryFolder testProjectDir = new TemporaryFolder()

	void setup() {
		final Path rootPath = testProjectDir.root.toPath()
		for (final String fileName : TEST_FILES) {
			final Path source = Paths.get(TEST_FILES_ROOT, fileName)
			final Path target = rootPath.resolve(fileName)

			Files.createDirectories(target.parent)

			try {
				Files.createLink(target, source)
			}
			catch (UnsupportedOperationException ignored) {
				Files.copy(source, target)
			}
		}
	}

	BuildResult run(GradleRunner runner) {
		try {
			final BuildResult result = runner.withProjectDir(testProjectDir.root).withPluginClasspath().build()

			println "-" * 30 + " Gradle Output " + "-" * 30
			println result.output
			println "-" * 30 + " Project Files " + "-" * 30
			return result
		}
		finally {
			testProjectDir.root.eachFileRecurse(FileType.FILES) {
				println it
			}
			println "-" * 75
		}
	}

	@CompileDynamic
	def generatesClasses() {
		when:
		def result = run(GradleRunner.create().withArguments('check'))

		then:
		result.task(":check").outcome == SUCCESS

		def mainOutputDir = new File(testProjectDir.root, 'build/classes/dyvil/main/')
		new File(mainOutputDir, 'org/example/Foo.class').exists()
		new File(mainOutputDir, 'org/example/Bar.class').exists()

		def testOutputDir = new File(testProjectDir.root, 'build/classes/dyvil/test/')
		new File(testOutputDir, 'org/example/FooTest.class').exists()

		def gensrcOutputDir = new File(testProjectDir.root, 'build/generated-src/gensrc/main/dyvil/')
		new File(gensrcOutputDir, 'org/example/Bar.dyv').exists()

		new File(testProjectDir.root, 'build/reports/tests/test/classes/org.example.FooTest.html').exists()
	}
}
