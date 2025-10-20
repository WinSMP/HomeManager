package org.winlogon.homemanager;

import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public class HomeManagerLoader implements PluginLoader {
    @Override 
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        var resolver = new MavenLibraryResolver();

        resolver.addRepository(
            new RemoteRepository.Builder(
                "central", 
                "default", 
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
            ).build()
        );

        resolver.addRepository(
            new RemoteRepository.Builder(
                "jitpack", 
                "default", 
                "https://jitpack.io"
            ).build()
        );

        resolver.addDependency(
            new Dependency(
                new DefaultArtifact("com.github.walker84837:JResult:1.4.0"),
                null
            )
        );

        resolver.addDependency(
            new Dependency(
                new DefaultArtifact("org.postgresql:postgresql:42.7.8"),
                null
            )
        );

        resolver.addDependency(
            new Dependency(
                new DefaultArtifact("org.xerial:sqlite-jdbc:3.45.3.0"),
                null
            )
        );

        classpathBuilder.addLibrary(resolver);
    }
}
