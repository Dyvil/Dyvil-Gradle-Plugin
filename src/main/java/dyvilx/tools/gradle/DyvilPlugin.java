package dyvilx.tools.gradle;

import dyvilx.tools.gradle.internal.DyvilVirtualDirectoryImpl;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.internal.JvmPluginsHelper;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DyvilPlugin implements Plugin<Project>
{
	public static final String DYVILC_MAIN = "dyvilx.tools.compiler.Main";
	public static final String GENSRC_MAIN = "dyvilx.tools.gensrc.Main";

	private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");

	@Override
	public void apply(Project project)
	{
		project.getPluginManager().apply(JavaPlugin.class);

		project
			.getExtensions()
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
		this.checkVersion(details, "org.dyvil", "compiler", "0.46.3",
		                  "Dyvil Compiler versions before 0.46.3 do not support the command-line syntax required by the plugin",
		                  (major, minor, patch) -> major > 0 || minor > 46 || minor == 46 && patch >= 3);
	}

	private void checkGenSrcVersion(DependencyResolveDetails details)
	{
		this.checkVersion(details, "org.dyvil", "gensrc", "0.12.0",
		                  "GenSrc versions before 0.12.0 do not support the command-line syntax required by the plugin",
		                  (major, minor, patch) -> major > 0 || minor > 10 || minor == 10 && patch >= 1);
	}

	interface VersionPredicate
	{
		boolean test(int major, int minor, int patch);
	}

	private void checkVersion(DependencyResolveDetails details, String group, String module, String replacementVersion,
		String reason, VersionPredicate predicate)
	{
		if (!group.equals(details.getRequested().getGroup()) || !module.equals(details.getRequested().getName()))
		{
			return;
		}

		final String version = details.getRequested().getVersion();
		if (version == null)
		{
			details.because("latest version").useVersion("+");
			return;
		}

		final Matcher matcher = VERSION_PATTERN.matcher(version);
		if (!matcher.find())
		{
			return;
		}

		try
		{
			final int major = Integer.parseInt(matcher.group(1));
			final int minor = Integer.parseInt(matcher.group(2));
			final int patch = Integer.parseInt(matcher.group(3));

			if (!predicate.test(major, minor, patch))
			{
				details.because(reason).useVersion(replacementVersion);
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
		final String sourceSetName = sourceSet.getName();
		final String compileTaskName = sourceSet.getCompileTaskName(languageName + "GenSrc");

		final File classesDir = project.file(
			project.getBuildDir() + "/classes/gensrc/" + languageName + "/" + sourceSetName);
		final File outputDir = project.file(
			project.getBuildDir() + "/generated/sources/gensrc/" + sourceSetName + "/" + languageName);

		project.getTasks().register(compileTaskName, GenSrcCompileTask.class, it -> {
			it.setDescription("Compiles the " + sourceSetName + " GenSrc files.");

			final Configuration gensrc = project.getConfigurations().getByName("gensrc");
			it.setClasspath(gensrc);
			it.setDyvilcClasspath(gensrc);

			it.setDestinationDir(classesDir);
			it.setSource(sourceDirSet);
		});

		final ConfigurableFileCollection classFiles = project.files(classesDir).builtBy(compileTaskName);

		final String runTaskName = sourceSet.getTaskName("generate", languageName + "GenSrc");
		project.getTasks().register(runTaskName, GenSrcRunTask.class, it -> {
			it.dependsOn(compileTaskName);
			it.setDescription("Generates the " + sourceSetName + " source files using GenSrc.");

			final Configuration gensrc = project.getConfigurations().getByName("gensrc");
			it.setClasspath(classFiles.plus(gensrc));

			it.setSourceDirs(sourceDirSet);
			it.setOutputDir(outputDir);
		});

		final ConfigurableFileCollection outputFiles = project.files(outputDir);
		sourceDirSet.srcDir(outputFiles);

		project.getTasks().named(sourceSet.getCompileTaskName(languageName), it -> it.dependsOn(runTaskName));
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
