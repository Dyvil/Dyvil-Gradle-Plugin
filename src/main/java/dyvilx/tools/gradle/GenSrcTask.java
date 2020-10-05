package dyvilx.tools.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.process.JavaExecSpec;

import java.io.File;

@NonNullApi
public class GenSrcTask extends DyvilCompileTask
{
	// =============== Fields ===============

	private final Property<File> classDestinationDir = this.getProject().getObjects().property(File.class);

	// =============== Constructors ===============

	public GenSrcTask()
	{
		this.include("**/*.dgt", "**/*.dgt", "**/*.dgc", "**/*.dgs");
	}

	// =============== Properties ===============

	@OutputDirectory
	public File getClassDestinationDir()
	{
		return this.classDestinationDir.get();
	}

	public void setClassDestinationDir(File classDestinationDir)
	{
		this.classDestinationDir.set(classDestinationDir);
	}

	public void setClassDestinationDir(Provider<? extends File> classDestionationDir)
	{
		this.classDestinationDir.set(classDestionationDir);
	}

	@Override
	public void setSource(Object source)
	{
		if (source instanceof SourceDirectorySet)
		{
			final SourceDirectorySet sourceDirectorySet = (SourceDirectorySet) source;
			super.setSource((Object) this
				.getProject()
				.files(sourceDirectorySet.getSrcDirs())
				.getAsFileTree()
				.matching(this.getPatternSet()));
			this.sourceDirs = sourceDirectorySet;
		}
		else
		{
			super.setSource(source);
		}
	}

	// =============== Methods ===============

	@Override
	public void copyTo(JavaExecSpec spec)
	{
		super.copyTo(spec);

		spec.setMain(DyvilPlugin.GENSRC_MAIN);

		spec.args("--output-dir=" + this.getClassDestinationDir());
		spec.args("--gensrc-dir=" + this.getDestinationDir());
		spec.args("test"); // TODO maybe run using gradle
	}

	@Override
	public void copyTo(JavaExec exec)
	{
		super.copyTo(exec);

		exec.getOutputs().dir(this.getClassDestinationDir());
	}
}
