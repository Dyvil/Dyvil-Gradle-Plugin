package dyvilx.tools.gradle;

import dyvilx.tools.gradle.internal.DyvilVirtualDirectoryImpl;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.internal.tasks.DefaultSourceSetOutput;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

class DyvilPlugin implements Plugin<Project>
{
	public static final String DYVILC_MAIN = "dyvilx.tools.compiler.Main";
	public static final String GENSRC_MAIN = "dyvilx.tools.gensrc.Main";

	@Override
	public void apply(Project project)
	{
		project.getPluginManager().apply(JavaPlugin.class);

		// configurations
		project.getConfigurations().register("dyvilc");
		project.getConfigurations().register("gensrc");

		for (final SourceSet sourceSet : project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets())
		{
			configureSourceSet(project, sourceSet);
		}
	}

	private static void configureSourceSet(Project project, SourceSet sourceSet)
	{
		final String srcDirName = "src/" + sourceSet.getName() + "/dyvil";
		final File outputDir = project.file(project.getBuildDir() + "/classes/dyvil/" + sourceSet.getName());

		// for each source set we will:
		// 1) create a new virtual directory mapping

		final DyvilVirtualDirectoryImpl directoryDelegate = new DyvilVirtualDirectoryImpl(sourceSet,
		                                                                                  project.getObjects());
		final SourceDirectorySet inputFiles = directoryDelegate.getDyvil();
		inputFiles.srcDir(srcDirName);

		new DslObject(sourceSet).getConvention().getPlugins().put(DyvilVirtualDirectory.NAME, directoryDelegate);

		// 2) create a dyvil compile task

		final String taskName = sourceSet.getCompileTaskName("dyvil");

		project.getTasks().register(taskName, DyvilCompileTask.class, it -> {
			it.setDescription("Compiles the " + sourceSet.getName() + " Dyvil code.");

			// 3) set up convention mapping for default sources (allows user to not have to specify)
			it.setClasspath(sourceSet.getCompileClasspath());
			it.setDyvilcClasspath(project.getConfigurations().getByName("dyvilc"));
			it.setDestinationDir(outputDir);
			it.setSource(directoryDelegate.getDyvil());
		});

		// 4) wire up inputs and outputs and task dependencies

		final ConfigurableFileTree outputFiles = project.fileTree(outputDir);

		inputFiles.setOutputDir(outputDir);
		outputFiles.include("**/*.class", "**/*.dyo");
		outputFiles.builtBy(taskName);

		((DefaultSourceSetOutput) sourceSet.getOutput()).addClassesDir(() -> outputDir);

		sourceSet.getAllSource().source(inputFiles);

		project.getTasks().named(sourceSet.getClassesTaskName(), it -> it.dependsOn(taskName));

		// 5) configure gensrc for each source directory set (only java and dyvil, for now)
		configureGenSrc(project, sourceSet, sourceSet.getJava());
		configureGenSrc(project, sourceSet, inputFiles);
	}

	private static void configureGenSrc(Project project, SourceSet sourceSet, SourceDirectorySet sourceDirSet)
	{
		final String languageName = sourceDirSet.getName();
		final String taskName = sourceSet.getCompileTaskName(languageName + "GenSrc");
		final File outputDir = project.file(
			project.getBuildDir() + "/generated-src/gensrc/" + sourceSet.getName() + "/" + languageName);

		project.getTasks().register(taskName, GenSrcTask.class, it -> {
			it.setDescription("Processes the " + sourceSet.getName() + " GenSrc files.");

			final Configuration gensrc = project.getConfigurations().getByName("gensrc");
			it.setClasspath(gensrc);
			it.setDyvilcClasspath(gensrc);

			it.setDestinationDir(outputDir);
			it.setSource(sourceDirSet);
		});

		sourceDirSet.srcDir(project.files(outputDir).builtBy(taskName));

		project.getTasks().named(sourceSet.getCompileTaskName(languageName), it -> it.dependsOn(taskName));
	}
}
