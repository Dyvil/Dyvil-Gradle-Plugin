package dyvilx.tools.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.process.JavaExecSpec;

@NonNullApi
public class GenSrcCompileTask extends DyvilCompileTask
{
	// =============== Constructors ===============

	public GenSrcCompileTask()
	{
		this.include("**/*.dgt", "**/*.dgt", "**/*.dgc", "**/*.dgs");
	}

	// =============== Properties ===============

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
	}
}
