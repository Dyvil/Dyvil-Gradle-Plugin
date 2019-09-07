package dyvilx.tools.gradle;

import dyvilx.tools.gradle.internal.DyvilVirtualDirectoryImpl;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.internal.JvmPluginsHelper;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
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

		project.getExtensions()
		       .add("convertDyvilCompileToJavaExec", (Action<String>) taskName -> convertToJavaExec(project, taskName));

		// configurations
		project.getConfigurations().register("dyvilc", it -> {
			it.setDescription("The Dyvil Compiler binaries to use for this project.");
			it.setVisible(false);

			it.getResolutionStrategy().eachDependency(this::checkCompilerVersion);
		});

		project.getConfigurations().register("gensrc", it -> {
			it.setDescription("The GenSrc binaries to use for this project.");
			it.setVisible(false);

			it.getResolutionStrategy().eachDependency(this::checkGenSrcVersion);
		});

		for (final SourceSet sourceSet : project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets())
		{
			configureSourceSet(project, sourceSet);
		}
	}

	private void checkCompilerVersion(DependencyResolveDetails details)
	{
		if (!"org.dyvil".equals(details.getRequested().getGroup()) //
		    || !"compiler".equals(details.getRequested().getName()))
		{
			return;
		}

		final String version = details.getRequested().getVersion();
		if (version == null)
		{
			details.because("latest version").useVersion("+");
			return;
		}

		final String[] split = version.split("\\.");
		try
		{
			final int major = Integer.parseInt(split[0]);
			final int minor = Integer.parseInt(split[1]);
			final int patch = Integer.parseInt(split[2]);

			if (major == 0 && (minor < 46 || minor == 46 && patch < 3))
			{
				details.because(
					"Dyvil Compiler versions before 0.46.3 do not support the command-line syntax required by the plugin")
				       .useVersion("0.46.3");
			}
		}
		catch (Exception ignored)
		{
			// invalid version notation, let gradle handle it.
		}
	}

	private void checkGenSrcVersion(DependencyResolveDetails details)
	{
		this.checkCompilerVersion(details);

		if (!"org.dyvil".equals(details.getRequested().getGroup()) //
		    || !"gensrc".equals(details.getRequested().getName()))
		{
			return;
		}

		final String version = details.getRequested().getVersion();
		if (version == null)
		{
			details.because("latest version").useVersion("+");
			return;
		}

		final String[] split = version.split("\\.");
		try
		{
			final int major = Integer.parseInt(split[0]);
			final int minor = Integer.parseInt(split[1]);
			final int patch = Integer.parseInt(split[2]);

			if (major == 0 && (minor < 10 || minor == 10 && patch < 1))
			{
				details.because(
					"GenSrc versions before 0.10.1 do not support the command-line syntax required by the plugin")
				       .useVersion("0.10.1");
			}
		}
		catch (Exception ignored)
		{
			// invalid version notation, let gradle handle it.
		}
	}

	private static void configureSourceSet(Project project, SourceSet sourceSet)
	{
		// for each source set we will:
		// 1) create a new virtual directory mapping

		final DyvilVirtualDirectoryImpl dyvilSourceSet = new DyvilVirtualDirectoryImpl(sourceSet, project.getObjects());
		new DslObject(sourceSet).getConvention().getPlugins().put(DyvilVirtualDirectory.NAME, dyvilSourceSet);

		final SourceDirectorySet sourceDirSet = dyvilSourceSet.getDyvil();
		sourceDirSet.srcDir("src/" + sourceSet.getName() + "/dyvil");

		// exclude dyvil sources from resources
		sourceSet.getResources().getFilter().exclude(e -> sourceDirSet.contains(e.getFile()));

		sourceSet.getAllJava().source(sourceDirSet);
		sourceSet.getAllSource().source(sourceDirSet);

		// 2) create a dyvil compile task

		final String taskName = sourceSet.getCompileTaskName("dyvil");

		Provider<DyvilCompileTask> compileTask = project.getTasks().register(taskName, DyvilCompileTask.class, it -> {
			JvmPluginsHelper.configureForSourceSet(sourceSet, sourceDirSet, it, it.getOptions(), project);

			// 3) set up convention mapping for default sources (allows user to not have to specify)
			it.dependsOn(sourceSet.getCompileJavaTaskName());
			it.setDyvilcClasspath(project.getConfigurations().getByName("dyvilc"));
			it.setSource(sourceDirSet);
		});

		JvmPluginsHelper.configureOutputDirectoryForSourceSet(sourceSet, sourceDirSet, project, compileTask,
		                                                      compileTask.map(DyvilCompileTask::getOptions));

		// 4) wire up inputs and outputs and task dependencies

		project.getTasks().named(sourceSet.getClassesTaskName(), it -> it.dependsOn(taskName));

		// 5) configure gensrc for each source directory set (only java and dyvil, for now)
		configureGenSrc(project, sourceSet, sourceSet.getJava());
		configureGenSrc(project, sourceSet, sourceDirSet);
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

			it.setClassDestinationDir(project.file(it.getTemporaryDir() + "/classes"));
			it.setDestinationDir(outputDir);
			it.setSource(sourceDirSet);
		});

		sourceDirSet.srcDir(project.files(outputDir).builtBy(taskName));

		project.getTasks().named(sourceSet.getCompileTaskName(languageName), it -> it.dependsOn(taskName));
	}

	private static void convertToJavaExec(Project project, String taskName)
	{
		final String newTaskName = taskName + "2";

		project.getTasks().named(taskName, DyvilCompileTask.class, it -> {
			it.setEnabled(false);
			it.dependsOn(newTaskName);
		});

		project.getTasks().register(newTaskName, JavaExec.class, exec -> {
			final DyvilCompileTask dyvilCompile = (DyvilCompileTask) project.getTasks().getByName(taskName);
			dyvilCompile.copyTo(exec);
		});
	}
}
