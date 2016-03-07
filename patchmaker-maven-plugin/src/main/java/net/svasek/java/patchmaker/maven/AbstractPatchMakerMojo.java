package net.svasek.java.patchmaker.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import java.util.List;
import java.util.Set;
import java.io.File;

public abstract class AbstractPatchMakerMojo extends AbstractMojo {
    /**
     * Used for resolving artifacts
     *
     * @component
     */
    ArtifactResolver resolver;

    /**
     * The local repository where the artifacts are located
     *
     * @parameter expression="localRepository"
     */
    ArtifactRepository localRepository;

    /**
     * The remote repositories where artifacts are located
     *
     * @parameter expression="project.remoteArtifactRepositories"
     */
    List<ArtifactRepository> remoteRepositories;

    /**
     * @component
     */
    ArtifactFactory artifactFactory;

    /**
     * The maven project.
     *
     * @parameter expression="project"
     * @required
     * @readonly
     */
    MavenProject project;

    /**
     *
     * @parameter expression="project.artifacts"
     * @readonly
     */
    Set<Artifact> projectArtifacts;

    /**
     * Dependency artifacts of this plugin.
     *
     * @parameter expression="plugin.artifacts"
     * @readonly
     */
    List<Artifact> pluginArtifacts;


    /**
     * Old artifact in format 'groupId:artifactId::classifier:type'
     * Version is taken from PROJECT dependencies.
     * @parameter
     */
    String oldArtifactRef;

    /**
     * New artifact in format 'groupId:artifactId::classifier:type'
     * Version is taken from PLUGIN dependencies.
     * @parameter
     */
    String newArtifactRef;

    /**
     * @parameter
     */
    private File oldFile;

    /**
     * @parameter
     */
    private File newFile;

    /**
     * @parameter
     */
    String distEarPath;

    /**
     * @parameter default-value="${project.build.directory}/descriptionTag"
     */
    File descFile;

    private File resolveArtifactRef(Iterable<Artifact> artifacts, String artifactRef) throws MojoFailureException, ArtifactResolutionException, ArtifactNotFoundException {
        for (Artifact artifact : artifacts) {
            final StringBuilder sb = new StringBuilder();
            sb.append(artifact.getGroupId());
            sb.append(':');
            sb.append(artifact.getArtifactId());
            sb.append(':');
            sb.append(':');
            if (artifact.getClassifier() != null) {
                sb.append(artifact.getClassifier());
            }
            sb.append(':');
            sb.append(artifact.getType());
            final String thisA = sb.toString();
            if (!thisA.equals(artifactRef)) continue;
            System.out.println("Resolving: " + artifact);
            resolver.resolve(artifact, remoteRepositories, localRepository);
            final File result = artifact.getFile();
            System.out.println(" ... found: " + result);
            return result;
        }
        throw new MojoFailureException("No such artifact found in a dependency list:" + artifactRef);
    }


    public File getOldFile() throws MojoFailureException, ArtifactResolutionException, ArtifactNotFoundException {
        if (oldFile == null) {
            if (oldArtifactRef == null) {
                throw new MojoFailureException("missing parameter - either oldFile or oldArtifactRef must be specified");
            }
            oldFile = resolveArtifactRef(projectArtifacts, oldArtifactRef);
        }
        return oldFile;
    }

    public File getNewFile() throws ArtifactResolutionException, ArtifactNotFoundException, MojoFailureException {
        if (newFile == null) {
            if (newArtifactRef == null) {
                throw new MojoFailureException("missing parameter - either newFile or newArtifactRef must be specified");
            }
            newFile = resolveArtifactRef(pluginArtifacts, newArtifactRef);
        }
        return newFile;
    }
}
