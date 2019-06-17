package dyvilx.tools.gradle

import groovy.io.FileType
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class FunctionalTest extends Specification {
	@Rule
	TemporaryFolder testProjectDir = new TemporaryFolder()

	def setup() {
		testProjectDir.newFile('settings.gradle') << /* language=Groovy */ """
		rootProject.name = 'test'
		"""

		testProjectDir.newFile('build.gradle') << /* language=Groovy */ """
		plugins {
			id 'java'
			id 'org.dyvil.dyvil-gradle'
		}
		
		repositories {
			jcenter()
		}
		
		dependencies {
			dyvilc group: 'org.dyvil', name: 'compiler', version: '0.45.0'
			gensrc group: 'org.dyvil', name: 'gensrc', version: '0.9.6'
			
			// https://mvnrepository.com/artifact/junit/junit
			testCompile group: 'junit', name: 'junit', version: '4.12'
		}
		"""

		testProjectDir.newFolder('src', 'main', 'dyvil', 'org', 'example')
		testProjectDir.newFile('src/main/dyvil/org/example/Foo.dyv') << /* language=dyvil */ """
		package org.example
		
		class Foo {
			func getText() -> String = "Hello World"
		}
		"""

		testProjectDir.newFile('src/main/dyvil/org/example/Bar.dyv.dgt') << /* language=dyvil */ """
		package org.example
		
		class Bar {
			#for(S <- [ "A", "B", "C" ]) {
				func get#(S)() -> String = "S"
			}
		}
		"""

		testProjectDir.newFolder('src', 'main', 'java', 'org', 'example')
		testProjectDir.newFile('src/main/java/org/example/Jav.java.dgt') << /* language=dyvil */ """
		package org.example;
		
		class Jav {
			#for(S <- [ "D", "E", "F"]) {
				String get#(S)() {
					return "S";
				}
			}
		}
		"""

		testProjectDir.newFolder('src', 'test', 'dyvil', 'org', 'example')
		testProjectDir.newFile('src/test/dyvil/org/example/FooTest.dyv') << /* language=dyvil */ """
		package org.example
		
		import org.junit.Assert
		import org.junit.Test
		
		class FooTest {
			@Test func getText() {
				let foo = new Foo
				Assert.assertEquals("Hello World", foo.getText())
			}
			
			@Test func getABC() {
				let bar = new Bar
				Assert.assertEquals("A", bar.getA())
				Assert.assertEquals("B", bar.getB())
				Assert.assertEquals("C", bar.getC())
			}
			
			@Test func getDEF() {
				let jav = new Jav
				Assert.assertEquals("D", jav.getD())
				Assert.assertEquals("E", jav.getE())
				Assert.assertEquals("F", jav.getF())
			}
		}
		"""
	}

	BuildResult run() {
		try {
			BuildResult result = GradleRunner.create()
					.withProjectDir(testProjectDir.root)
					.withArguments('check')
					.withPluginClasspath()
					.build()

			println "-" * 30 + ' Gradle Output ' + "-" * 30
			println result.output
			println "-" * 30 + ' Project Files ' + "-" * 30
			return result
		}
		finally {
			testProjectDir.root.eachFileRecurse(FileType.FILES) {
				println it
			}
			println "-" * 75
		}
	}

	def generatesClasses() {
		when:
		def result = run()

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
