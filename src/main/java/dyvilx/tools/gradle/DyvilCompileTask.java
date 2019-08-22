package dyvilx.tools.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.process.JavaExecSpec;
import org.gradle.util.RelativePathUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@NonNullApi
public class DyvilCompileTask extends AbstractCompile
{
	// =============== Fields ===============

	private FileCollection dyvilcClasspath;

	protected SourceDirectorySet sourceDirs;

	private List<String> extraArgs = new ArrayList<>();

	// =============== Properties ===============

	@Override
	@InputFiles
	@SkipWhenEmpty
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource()
	{
		return super.getSource();
	}

	@Override
	public void setSource(Object source)
	{
		super.setSource(source);
		if (source instanceof SourceDirectorySet)
		{
			this.sourceDirs = (SourceDirectorySet) source;
		}
		else
		{
			this.sourceDirs = null;
		}
	}

	protected PatternFilterable getPatternSet()
	{
		try
		{
			final Field patternSet = SourceTask.class.getDeclaredField("patternSet");
			patternSet.setAccessible(true);
			return (PatternFilterable) patternSet.get(this);
		}
		catch (IllegalAccessException | NoSuchFieldException e)
		{
			throw new UnsupportedOperationException(
				"SourceTask.patternSet not accessible, please update the Dyvil plugin", e);
		}
	}

	@Classpath
	public FileCollection getDyvilcClasspath()
	{
		return this.dyvilcClasspath;
	}

	public void setDyvilcClasspath(FileCollection dyvilcClasspath)
	{
		this.dyvilcClasspath = dyvilcClasspath;
	}

	// --------------- Options ---------------

	// --------------- Extra Args ---------------

	@Input
	public List<String> getExtraArgs()
	{
		return this.extraArgs;
	}

	public void setExtraArgs(List<String> extraArgs)
	{
		Objects.requireNonNull(extraArgs);
		this.extraArgs = extraArgs;
	}

	public void extraArgs(Object... extraArgs)
	{
		for (final Object extraArg : extraArgs)
		{
			this.extraArgs.add(extraArg.toString());
		}
	}

	public void extraArgs(Iterable<?> extraArgs)
	{
		for (final Object extraArg : extraArgs)
		{
			this.extraArgs.add(extraArg.toString());
		}
	}

	// =============== Methods ===============

	@Override
	@TaskAction
	protected void compile()
	{
		this.getProject().javaexec(this::configure);
	}

	protected void configure(JavaExecSpec spec)
	{
		spec.setClasspath(this.getDyvilcClasspath());
		spec.setMain(DyvilPlugin.DYVILC_MAIN);

		spec.args(this.getExtraArgs());

		// TODO the compiler has this hardcoded. should probably use File.pathSeparator here and there.
		final String pathSeparator = ":";
		final File workingDir = spec.getWorkingDir();

		spec.args("source_dirs=" + this.sourceDirs.getSrcDirs().stream()
		                                          .map(f -> RelativePathUtil.relativePath(workingDir, f))
		                                          .collect(Collectors.joining(pathSeparator)));
		spec.args("output_dir=" + this.getDestinationDir());
		spec.args("libraries=" + this.getClasspath().getFiles().stream().map(File::getPath)
		                             .collect(Collectors.joining(pathSeparator)));

		final PatternFilterable filter = this.getPatternSet();
		final Set<String> includes = filter.getIncludes();
		final Set<String> excludes = filter.getExcludes();
		if (!includes.isEmpty())
		{
			spec.args("includes=" + String.join(pathSeparator, includes));
		}
		if (!excludes.isEmpty())
		{
			spec.args("excludes=" + String.join(pathSeparator, excludes));
		}

		spec.args("compile", "--ansi", "--machine-markers");
	}
}
