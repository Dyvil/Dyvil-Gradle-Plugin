package dyvilx.tools.gradle;

import org.gradle.api.file.FileTree;
import org.gradle.api.file.RelativePath;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenSrcRunTask extends JavaExec
{
	protected SourceDirectorySet sourceDirs;
	protected File outputDir;

	public GenSrcRunTask()
	{
		this.setMain("dyvilx.tools.gensrc.Runner");
		this.getArgumentProviders().add(this::collectArgs);
	}

	private List<String> collectArgs()
	{
		final List<String> args = new ArrayList<>();

		for (final File sourceDir : this.getSourceDirs())
		{
			args.add("--source-dir=" + sourceDir);
		}

		args.add("--output-dir=" + this.getOutputDir());

		final FileTree files = this.getAllSource();

		final FileTree templates = files.matching(it -> it.include("**/*.dgt"));
		templates.visit(details -> {
			if (!details.isDirectory())
			{
				args.add(templateName(details.getRelativePath()));
			}
		});

		final FileTree specs = files.matching(it -> it.include("**/*.dgs"));
		specs.visit(details -> {
			if (!details.isDirectory())
			{
				args.add(details.getRelativePath().getPathString());
			}
		});

		return args;
	}

	private static String templateName(RelativePath relativePath)
	{
		// org/example/Foo.dyv.dgt -> org.example.Foo_dyv

		final String[] segments = relativePath.getSegments();
		final int last = segments.length - 1;
		final StringBuilder templateName = new StringBuilder();

		for (int i = 0; i < last; i++)
		{
			templateName.append(segments[i]).append('.');
		}

		templateName.append(segments[last].replace('.', '_'), 0, segments[last].length() - 4);

		return templateName.toString();
	}

	private FileTree getAllSource()
	{
		return this.getProject().files(this.getSourceDirs().getSrcDirs()).getAsFileTree();
	}

	@InputFiles
	@SkipWhenEmpty
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource()
	{
		return this.getAllSource().matching(it -> it.include("**/*.dgt", "**/*.dgs"));
	}

	public SourceDirectorySet getSourceDirs()
	{
		return sourceDirs;
	}

	public void setSourceDirs(SourceDirectorySet sourceDirs)
	{
		this.sourceDirs = sourceDirs;
	}

	@OutputDirectory
	public File getOutputDir()
	{
		return outputDir;
	}

	public void setOutputDir(File outputDir)
	{
		this.outputDir = outputDir;
	}
}
