package dyvilx.tools.gradle;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;

public interface DyvilVirtualDirectory
{
	String NAME = "dyvil";

	SourceDirectorySet getDyvil();

	DyvilVirtualDirectory dyvil(Closure configureClosure);

	DyvilVirtualDirectory dyvil(Action<? super SourceDirectorySet> configureAction);
}
