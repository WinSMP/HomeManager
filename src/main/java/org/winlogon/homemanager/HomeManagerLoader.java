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
                "https://repo.maven.apache.org/maven2/"
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
                new DefaultArtifact("com.github.walker84837:JResult:1.3.0"),
                null
            )
        );

        resolver.addDependency(
            new Dependency(
                new DefaultArtifact("org.postgresql:postgresql:42.7.5"),
                null
            )
        );

        classpathBuilder.addLibrary(resolver);
    }
}
