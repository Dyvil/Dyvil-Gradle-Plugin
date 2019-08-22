package dyvilx.tools.gradle.internal;

import dyvilx.tools.gradle.DyvilVirtualDirectory;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.DefaultSourceSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.SourceSet;
import org.gradle.util.ConfigureUtil;

public class DyvilVirtualDirectoryImpl implements DyvilVirtualDirectory, HasPublicType
{
	private final SourceDirectorySet dyvil;

	public DyvilVirtualDirectoryImpl(SourceSet parent, ObjectFactory objectFactory)
	{
		final String displayName = ((DefaultSourceSet) parent).getDisplayName() + " Dyvil source";
		this.dyvil = objectFactory.sourceDirectorySet(DyvilVirtualDirectory.NAME, displayName);
		this.dyvil.getFilter().include("**/*.dyv", "**/*.dyh", "**/*.dyvil", "**/*.dyvilh");
	}

	@Override
	public SourceDirectorySet getDyvil()
	{
		return this.dyvil;
	}

	@Override
	public DyvilVirtualDirectory dyvil(Closure configureClosure)
	{
		ConfigureUtil.configure(configureClosure, this.getDyvil());
		return this;
	}

	@Override
	public DyvilVirtualDirectory dyvil(Action<? super SourceDirectorySet> configureAction)
	{
		configureAction.execute(this.getDyvil());
		return this;
	}

	@Override
	public TypeOf<?> getPublicType()
	{
		return TypeOf.typeOf(DyvilVirtualDirectory.class);
	}
}
