package dyvilx.tools.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.process.JavaExecSpec;

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

	private final CompileOptions compileOptions;

	private List<String> extraArgs = new ArrayList<>();

	// =============== Constructors ===============

	public DyvilCompileTask()
	{
		this.compileOptions = this.getServices().get(ObjectFactory.class).newInstance(CompileOptions.class);
	}

	// =============== Properties ===============

	@Nested
	public CompileOptions getOptions()
	{
		return this.compileOptions;
	}

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

	@Internal
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

	@TaskAction
	protected void compile()
	{
		this.getProject().javaexec(this::copyTo);
	}

	public void copyTo(JavaExecSpec spec)
	{
		spec.setClasspath(this.getDyvilcClasspath());
		spec.setMain(DyvilPlugin.DYVILC_MAIN);

		spec.args(this.getExtraArgs());

		spec.args("--source-dirs=" + this.sourceDirs.getSrcDirs().stream().map(File::getPath)
		                                            .collect(Collectors.joining(File.pathSeparator)));
		spec.args("--output-dir=" + this.getDestinationDir());
		spec.args("--classpath=" + this.getClasspath().getAsPath());

		final PatternFilterable filter = this.getPatternSet();
		final Set<String> includes = filter.getIncludes();
		final Set<String> excludes = filter.getExcludes();
		if (!includes.isEmpty())
		{
			spec.args("--include-patterns=" + String.join(File.pathSeparator, includes));
		}
		if (!excludes.isEmpty())
		{
			spec.args("--exclude-patterns=" + String.join(File.pathSeparator, excludes));
		}

		spec.args("compile", "--ansi", "--marker-style=machine");
	}

	public void copyTo(JavaExec exec)
	{
		this.copyTo((JavaExecSpec) exec);

		exec.getInputs().files(this.getSource());
		exec.getInputs().file(this.getClasspath());
		exec.getOutputs().dir(this.getDestinationDir());
	}
}
