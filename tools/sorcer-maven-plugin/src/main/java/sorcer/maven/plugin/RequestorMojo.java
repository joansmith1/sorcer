/*
 * Copyright 2013 Rafał Krupiński.
 * Copyright 2013 Sorcersoft.com S.A.
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

package sorcer.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.InstantiationStrategy;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.slf4j.impl.StaticLoggerBinder;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import sorcer.core.SorcerEnv;
import sorcer.launcher.SorcerProcessBuilder;
import sorcer.maven.util.ArtifactUtil;
import sorcer.util.Process2;
import sorcer.maven.util.TestCycleHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static sorcer.core.SorcerConstants.*;
import static sorcer.util.JavaSystemProperties.RMI_SERVER_USE_CODEBASE_ONLY;
import static sorcer.util.JavaSystemProperties.SECURITY_POLICY;
import static sorcer.util.JavaSystemProperties.UTIL_LOGGING_CONFIG_FILE;

/**
 * @author Rafał Krupiński
 */
@Execute(phase = LifecyclePhase.PACKAGE)
@Mojo(name = "run-requestor", defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, instantiationStrategy = InstantiationStrategy.PER_LOOKUP)
public class RequestorMojo extends AbstractSorcerMojo {

	/**
	 * mainClass must be set ether here or in requestors list
	 */
	@Parameter(property = "sorcer.requestor.mainClass")
	protected String mainClass;

	@Parameter(defaultValue = "${project.build.directory}/sorcer.env")
	protected File sorcerEnvFile;

	@Parameter(defaultValue = "test")
	protected String scope;

	@Parameter(property = "sorcer.requestor.debug")
	protected boolean debug;

	@Parameter(property = "sorcer.provider.debug")
	protected boolean serverDebug;

	@Parameter
	protected String[] requestorCodebase = new String[0];

	@Parameter
	protected String[] requestorClasspath = new String[0];

	@Parameter(defaultValue = "${project.build.directory}/requestor%s.log")
	protected String logFile;

	@Parameter
	protected ClientConfiguration[] requestors;

	@Parameter(defaultValue = "${env.SORCER_HOME}")
	protected File sorcerHome;

	/**
	 * Milliseconds to wait before stopping the requestor
	 */
	@Parameter(defaultValue = "60000", property = "client.timeout")
	protected int timeout;

	@Parameter(property = "project.build.outputDirectory")
	protected File workingDir;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
        StaticLoggerBinder.getSingleton().setLog(getLog());
		if (!project.getPackaging().equals("jar")) {
			getLog().warn("Plugin misconfigured: running on a project of packaging other than jar");
		}

		Map<String, String> defaultSystemProps = new HashMap<String, String>();
		defaultSystemProps.put(UTIL_LOGGING_CONFIG_FILE, new File(sorcerHome, "configs/sorcer.logging").getPath());
		defaultSystemProps.put(SECURITY_POLICY, new File(testOutputDir, "sorcer.policy").getPath());
		defaultSystemProps.put(S_KEY_SORCER_ENV, sorcerEnvFile.getPath());
		defaultSystemProps.put(RMI_SERVER_USE_CODEBASE_ONLY, "false");
        defaultSystemProps.put(S_WEBSTER_INTERFACE, getInetAddress());
        defaultSystemProps.put(SORCER_HOME, sorcerHome.getPath());

		List<ClientRuntimeConfiguration> configurations = buildClientsList();
		for (int i = 0; i < configurations.size(); i++) {
			ClientRuntimeConfiguration config = configurations.get(i);
            Map<String, String> properties = new HashMap<String, String>();
			properties.putAll(defaultSystemProps);
            properties.putAll(config.getSystemProperties());

            SorcerProcessBuilder builder = new SorcerProcessBuilder(new SorcerEnv(properties));
            config.preconfigureProcess(builder);
            //not all properties are currently supported by SorcerEnv
            builder.setProperties(properties);

			builder.setWorkingDir(workingDir);
			builder.setDebugger(debug);
			builder.setOutFile(getLogFile(configurations, i));
			if (config.arguments != null) {
				builder.setParameters(Arrays.asList(config.arguments));
			}

			Process2 process;
			try {
				getLog().info("Starting client process");
				process = builder.startProcess();
				Integer exitCode;
                try {
                    if (debug || serverDebug) {
                        exitCode = process.waitFor();
                    } else {
                        exitCode = process.waitFor(timeout);
                    }
                } catch (InterruptedException e) {
                    process.destroy();
                    throw new MojoExecutionException(e.getMessage(), e);
                }

                if (new Integer(0).equals(exitCode)) {
					getLog().info("Client process has finished successfully");
				} else {
					TestCycleHelper.getInstance().setFail();
					if (exitCode == null) {
						getLog().warn("Client process has been destroyed after reaching a timeout");
					} else {
						getLog().warn("Client process has finished with exit code = " + exitCode);
					}
				}
			} catch (IllegalStateException x) {
				//fail in DestroyMojo, don't log stack trace, there is nothing interesting
				getLog().warn(x.getMessage());
				TestCycleHelper.getInstance().setFail();
			} catch (IOException x) {
                getLog().warn("Could not start client process", x);
                TestCycleHelper.getInstance().setFail();
            }
        }
	}

    private List<ClientRuntimeConfiguration> buildClientsList() throws MojoExecutionException {
		if (requestors == null || requestors.length == 0) {
			ClientRuntimeConfiguration config = new ClientRuntimeConfiguration(mainClass, buildClasspath(requestorClasspath));
			updateCodebaseAndRoots(config, requestorCodebase);
			return Arrays.asList(config);
		} else {
			List<ClientRuntimeConfiguration> result = new ArrayList<ClientRuntimeConfiguration>(requestors.length);
			for (ClientConfiguration requestor : requestors) {
				String configMainClass = mainClass != null ? mainClass : requestor.mainClass;
				String[] userClasspath = requestor.classpath != null ? requestor.classpath : requestorClasspath;
				ClientRuntimeConfiguration config = new ClientRuntimeConfiguration(configMainClass, buildClasspath(userClasspath));

				String[] userCodebase = requestor.codebase != null ? requestor.codebase : requestorCodebase;
				updateCodebaseAndRoots(config, userCodebase);
				config.arguments = requestor.arguments;
				result.add(config);
			}
			return result;
		}
	}

	private Collection<String> buildClasspath(String[] userClasspath) throws MojoExecutionException {
		Collection<Artifact> artifacts = resolveDependencies(KEY_REQUESTOR, userClasspath, scope);
		Collection<String> classPathList = ArtifactUtil.toString(artifacts);
		classPathList.add(project.getBuild().getTestOutputDirectory());
		return classPathList;
	}

	private File getLogFile(List<ClientRuntimeConfiguration> configurations, int index) {
		if (configurations.size() == 1) {
			return new File(String.format(logFile, ""));
		} else {
			return new File(String.format(logFile, "" + index));
		}
	}

	private void updateCodebaseAndRoots(ClientRuntimeConfiguration config, String[] userCodebase) throws MojoExecutionException {
		Collection<Artifact> artifacts = resolveDependencies(KEY_REQUESTOR, userCodebase, scope);
		List<String> codebase = new LinkedList<String>();
		try {
			String repositoryPath = repositorySystemSession.getLocalRepository().getBasedir().getCanonicalPath();
			List<String> websterRoots = new LinkedList<String>();
			for (Artifact artifact : artifacts) {
				File artifactFile = artifact.getFile();
				String artifactPath = artifactFile.getCanonicalPath();
				if (artifactPath.startsWith(repositoryPath)) {
					// add jar from repository
					codebase.add(artifactPath.substring(repositoryPath.length()));
				} else {
					// add jar from target dir
					codebase.add(artifactFile.getName());
					websterRoots.add(artifactFile.getParent());
				}
			}

            websterRoots.add(repositorySystemSession.getLocalRepository().getBasedir().getPath());
			config.setWebsterRoots(websterRoots);
			config.codebase = codebase.toArray(new String[codebase.size()]);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	protected Collection<Artifact> resolveDependencies(String propertyKey, String[] userEntries, String scope)
			throws MojoExecutionException {
		List<String> coordinates = new LinkedList<String>();
		if (propertyKey != null && project.getProperties().containsKey(propertyKey)) {
			coordinates.add(project.getProperties().getProperty(propertyKey));
		}
		if (userEntries != null) {
			coordinates.addAll(Arrays.asList(userEntries));
		}
		try {
			Set<Artifact> artifacts = new HashSet<Artifact>(coordinates.size());
			for (String coords : coordinates) {
				artifacts.addAll(resolveDependencies(new DefaultArtifact(coords), scope));
			}
			return artifacts;
		} catch (DependencyResolutionException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
