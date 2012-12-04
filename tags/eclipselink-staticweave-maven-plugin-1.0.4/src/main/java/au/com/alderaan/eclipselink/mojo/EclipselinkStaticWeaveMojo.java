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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.tools.weaving.jpa.StaticWeaveProcessor;

import java.io.File;
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
 * @author Craig Day <craigday@gmail.com>
 * @goal weave
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
public class EclipselinkStaticWeaveMojo extends AbstractMojo {
    /**
     * @parameter expression="${weave.persistenceInfo}"
     */
    private String persistenceInfo;

    /**
     * @parameter expression="${weave.persistenceXMLLocation}"
     */
    private String persistenceXMLLocation;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     */
    private String source;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     */
    private String target;

    /**
     * @parameter default-value="OFF"
     */
    private String logLevel = SessionLog.OFF_LABEL;

    /**
     * The maven project descriptor
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Include the project output directory and class path. This works around a bug in Eclipselink that uses
     * {@link Class#forName(String)} without referencing a class loader that knows about project classes.
     * Offending method is
     * {@link org.eclipse.persistence.internal.libraries.asm.ClassWriter#getCommonSuperClass(String, String)}
     * and should be fixed in Eclipselink 2.3.3.
     *
     * @parameter default-value="false"
     */
    private boolean includeProjectClasspath = false;

    public void execute() throws MojoExecutionException {
        try {
            if (includeProjectClasspath) {
                // Thread context class loader is the ClassRealm for this plugin.
                ClassRealm c = (ClassRealm) Thread.currentThread().getContextClassLoader();
                // Add Project output directory to class path.
                c.addURL(new File(project.getBuild().getOutputDirectory()).toURI().toURL());
                // Add Project class path to class path.
                for (URL url : buildClassPath()) {
                    c.addURL(url);
                }
            }

            StaticWeaveProcessor weave = new StaticWeaveProcessor(source, target);
            URL[] urls = buildClassPath();
            if (urls.length > 0) {
                URLClassLoader classLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
                weave.setClassLoader(classLoader);
            }
            if (persistenceInfo != null) {
                weave.setPersistenceInfo(persistenceInfo);
            }
            if (persistenceXMLLocation != null) {
                weave.setPersistenceXMLLocation(persistenceXMLLocation);
            }
            weave.setLog(new PrintWriter(System.out));
            weave.setLogLevel(getLogLevel());
            weave.performWeaving();
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Failed", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed", e);
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Failed", e);
        }
    }

    private int getLogLevel() {
        return AbstractSessionLog.translateStringToLoggingLevel(logLevel);
    }

    public void setLogLevel(String logLevel) {
        if (SessionLog.OFF_LABEL.equalsIgnoreCase(logLevel) ||
                SessionLog.SEVERE_LABEL.equalsIgnoreCase(logLevel) ||
                SessionLog.WARNING_LABEL.equalsIgnoreCase(logLevel) ||
                SessionLog.INFO_LABEL.equalsIgnoreCase(logLevel) ||
                SessionLog.CONFIG_LABEL.equalsIgnoreCase(logLevel) ||
                SessionLog.FINE_LABEL.equalsIgnoreCase(logLevel) ||
                SessionLog.FINER_LABEL.equalsIgnoreCase(logLevel) ||
                SessionLog.FINEST_LABEL.equalsIgnoreCase(logLevel) ||
                SessionLog.ALL_LABEL.equalsIgnoreCase(logLevel)) {
            this.logLevel = logLevel.toUpperCase();
        } else {
            throw new IllegalArgumentException("Unknown log level: " + logLevel);
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
