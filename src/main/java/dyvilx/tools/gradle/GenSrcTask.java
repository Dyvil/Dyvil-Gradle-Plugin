package dyvilx.tools.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.process.JavaExecSpec;

import java.io.File;

@NonNullApi
public class GenSrcTask extends DyvilCompileTask
{
	public GenSrcTask()
	{
		this.include("**/*.dgt", "**/*.dgt", "**/*.dgc");
	}

	@Override
	public void setSource(Object source)
	{
		if (source instanceof SourceDirectorySet)
		{
			final SourceDirectorySet sourceDirectorySet = (SourceDirectorySet) source;
			super.setSource((Object) this.getProject().files(sourceDirectorySet.getSrcDirs()).getAsFileTree()
			                             .matching(this.getPatternSet()));
			this.sourceDirs = sourceDirectorySet;
		}
		else
		{
			super.setSource(source);
		}
	}

	@Override
	protected void configure(JavaExecSpec spec)
	{
		super.configure(spec);

		spec.setMain(DyvilPlugin.GENSRC_MAIN);

		spec.args("--output-dir=" + this.getTemporaryDir() + File.separatorChar + "classes");
		spec.args("--gensrc-dir=" + this.getDestinationDir());
		spec.args("test"); // TODO maybe run using gradle
	}
}
