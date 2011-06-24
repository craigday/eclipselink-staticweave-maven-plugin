package au.com.alderaan.eclipselink.mojo;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.tools.weaving.jpa.StaticWeaveProcessor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Goal which performs Eclipselink static weaving.
 *
 * @goal weave
 * @phase process-classes
 * @requiresDependencyResolution compile
 *
 * @author Craig Day <craigday@gmail.com>
 */
public class EclipselinkStaticWeaveMojo extends AbstractMojo {
    /**
     * @parameter expression="${weave.persistenceInfo}
     */
    private String persistenceInfo;

    /**
     * @parameter expression="${project.build.outputDirectory}
     */
    private String source;

    /**
     * @parameter expression="${project.build.outputDirectory}
     */
    private String target;

    /**
     * The maven project descriptor
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        Log log = getLog();
        try {
            StaticWeaveProcessor weave = new StaticWeaveProcessor(source, target);
            URL[] urls = buildClassPath();
            if (urls != null) {
                URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
                weave.setClassLoader(classLoader);
            }
            if (persistenceInfo != null) {
                weave.setPersistenceInfo(persistenceInfo);
            }
            weave.setLog(new PrintWriter(System.out));
            weave.setLogLevel(SessionLog.ALL);
            weave.performWeaving();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Failed", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed", e);
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Failed", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private URL[] buildClassPath() throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        Set<Artifact> artifacts = (Set<Artifact>) project.getArtifacts();
        for (Artifact a : artifacts) {
            urls.add(a.getFile().toURI().toURL());
        }
        return urls.toArray(new URL[urls.size()]);
    }

}
