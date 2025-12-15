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

        var repos = new RemoteRepository[] {
            addRepository("central", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR),
            addRepository("jitpack", "https://jitpack.io"),
        };

        var deps = new Dependency[] {
            addDependency("com.github.walker84837", "JResult", "1.4.0"),
            addDependency("org.postgresql", "postgresql", "42.7.8"),
            addDependency("org.xerial", "sqlite-jdbc", "3.50.3.0"),
        };

        for (var repository : repos) {
            resolver.addRepository(repository);
        }

        for (var dependency : deps) {
            resolver.addDependency(dependency);
        }

        classpathBuilder.addLibrary(resolver);
    }

    private Dependency addDependency(String groupId, String artifactId, String version) {
        return new Dependency(new DefaultArtifact(groupId + ":" + artifactId + ":" + version), null);
    }

    private RemoteRepository addRepository(String id, String url) {
        return new RemoteRepository.Builder(id, "default", url).build();
    }
}
