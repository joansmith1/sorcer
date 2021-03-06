/*
 * Copyright 2008 the original author or authors.
 * Copyright 2013, 2014 Sorersoft.com S.A.
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
package sorcer.provider.boot;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jini.start.ServiceDescriptor;
import sorcer.core.SorcerEnv;
import sorcer.resolver.Resolver;
import sorcer.util.ArtifactCoordinates;

import static sorcer.util.Artifact.sorcer;
import static sorcer.util.ArtifactCoordinates.coords;

/**
 * Holds static attributes used during the startup of services and provides
 * utilities to obtain {@link com.sun.jini.start.ServiceDescriptor} instances
 * for SORCER services
 */
public class SorcerDescriptorUtil {
    final static Logger logger = LoggerFactory.getLogger("sorcer.provider.boot");
    public static final ArtifactCoordinates SOS_PLATFORM = sorcer("sos-platform");
    public static final ArtifactCoordinates EXERTMONITOR_SERVICE = sorcer("exertmonitor-prv");
    public static final ArtifactCoordinates EXERTLET_UI = sorcer("sos-exertlet-sui");
    public static final ArtifactCoordinates DBP_PRV = sorcer("dbp-prv");
    public static final ArtifactCoordinates DSP_PRV = sorcer("dsp-prv");
    public static final ArtifactCoordinates CATALOGER_PRV = sorcer("cataloger-prv");
    public static final ArtifactCoordinates LOGGER_PRV = sorcer("logger-prv");
    public static final ArtifactCoordinates LOGGER_SUI = sorcer("logger-sui");
    public static final ArtifactCoordinates JOBBER_PRV = sorcer("rendezvous-prv");

    public static final ArtifactCoordinates SLEEPYCAT = coords("com.sleepycat:je");
    public static final ArtifactCoordinates SERVICEUI = coords("net.jini.lookup:serviceui");

	private static String sorcerHome = SorcerEnv.getHomeDir().getAbsolutePath();

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.ServiceJobber} using the Webster port created
	 * by this utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param monitorConfig
	 *            The configuration options the Monitor will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>monitor.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getJobber(String policy,
			String... jobberConfig) throws IOException {
		return (getJobber(policy, Booter.getPort(), jobberConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.ServiceJobber}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param jobberConfig
	 *            The configuration options the Monitor will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>monitor.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getJobber(String policy, int port,
			String... jobberConfig) throws IOException {
		return (getJobber(policy, Booter.getHostAddress(), port, jobberConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.ServiceJobber}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param hostAddress
	 *            The address to use when constructing the codebase
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param jobberConfig
	 *            The configuration options the Monitor will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>monitor.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getJobber(String policy,
			String hostAddress, int port, String... jobberConfig)
			throws IOException {
		if (sorcerHome == null)
			throw new RuntimeException("'sorcer.home' property not declared");
		
		// service provider classpath
		String jobberClasspath = Resolver.resolveClassPath(JOBBER_PRV, SOS_PLATFORM);
		
		// service provider codebase
        String jobberCodebase = getCodebase(new ArtifactCoordinates[]{
				SOS_PLATFORM,
				//SOS_ENV,
				SERVICEUI,
				EXERTLET_UI,
		}, hostAddress, Integer.toString(port));
		String implClass = "sorcer.core.provider.rendezvous.ServiceJobber";
		return (new SorcerServiceDescriptor(jobberCodebase, policy,
				jobberClasspath, implClass, jobberConfig));

	}
	
	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.ServiceTasker} with beaned
	 * {@link sorcer.util.ExertProcessor}, called Exerter.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param exertmonitorConfig
	 *            The configuration file the ExertMonitor will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>exertmonitor.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getExerter(String policy,
			String... exerterConfig) throws IOException {
		return (getExerter(policy, Booter.getPort(), exerterConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.ServiceTasker} with beaned
	 * {@link sorcer.util.ExertProcessor}, called Exerter.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param exertConfig
	 *            The configuration options the ExertProcessor provider will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Exerter using an anonymous port. The <tt>sorcer-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib/sorcer/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getExerter(String policy, int port,
			String... exertConfig) throws IOException {
		return (getJobber(policy, Booter.getHostAddress(), port, exertConfig));

	}
	
	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.ServiceTasker} with beaned
	 * {@link sorcer.util.ExertProcessor}, called Exerter.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param hostAddress
	 *            The address to use when constructing the codebase
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param exertConfig
	 *            The configuration options the ExertProcessor provider will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Exerter using an anonymous port. The <tt>sorcer-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib/sorcer/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getExerter(String policy,
			String hostAddress, int port, String... exerterConfig)
			throws IOException {
		return (getExertMonitor(policy, exerterConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.exertmonitor.ExertMonitor} using the Webster port created
	 * by this utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param exertmonitorConfig
	 *            The configuration options the ExertMonitor will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>exertmonitor.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getExertMonitor(String policy,
			String... exertmonitorConfig) throws IOException {
		return (getExertMonitor(policy, Booter.getPort(), exertmonitorConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.exertmonitor.ExertMonitor}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param exertmonitorConfig
	 *            The configuration options the ExertMonitor will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>exertmonitor.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getExertMonitor(String policy, int port,
			String... exertmonitorConfig) throws IOException {
		return (getExertMonitor(policy, Booter.getHostAddress(), port, exertmonitorConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.exertmonitor.ExertMonitor}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param hostAddress
	 *            The address to use when constructing the codebase
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param exertmonitorConfig
	 *            The configuration options the ExertMonitor will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>exertmonitor.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getExertMonitor(String policy,
			String hostAddress, int port, String... exertmonitorConfig)
			throws IOException {
		if (sorcerHome == null)
			throw new RuntimeException("'sorcer.home' property not declared");


		// service provider classpath
		String exertmonitorClasspath = Resolver.resolveClassPath(
                //SOS_PLATFORM,
				EXERTMONITOR_SERVICE,
				SLEEPYCAT,
				SOS_PLATFORM
		);

	// service provider codebase
        String exertmonitorCodebase = getCodebase(new ArtifactCoordinates[]{
				SOS_PLATFORM,
				//SOS_ENV,
				SERVICEUI,
				EXERTLET_UI,
		}, hostAddress, Integer.toString(port));


		// service provider codebase
		String implClass = "sorcer.core.provider.exertmonitor.ExertMonitor";
		return (new SorcerServiceDescriptor(exertmonitorCodebase, policy,
				exertmonitorClasspath, implClass, exertmonitorConfig));

	}
	
	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * DatabaseStorerusing the Webster port created
	 * by this utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param sdbConfig
	 *            The configuration file the DatabaseStore will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         ObjectStore using an anonymous port. The <tt>sdb-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getDatabaseStorer(String policy, String sdbConfig)
			throws IOException {
		return (getDatabaseStorer(policy, new String[] { sdbConfig }));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.DatabaseStorer} using the Webster port created
	 * by this utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param sdbConfig
	 *            The configuration options the DatabaseStore will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>sdb-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getDatabaseStorer(String policy,
			String... sdbConfig) throws IOException {
		return (getDatabaseStorer(policy, Booter.getPort(), sdbConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.DatabaseStorer}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param sdbConfig
	 *            The configuration options the DatabaseStore will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>sdb-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getDatabaseStorer(String policy, int port,
			String... sdbConfig) throws IOException {
		return (getDatabaseStorer(policy, Booter.getHostAddress(), port, sdbConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.DatabaseStorer}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param hostAddress
	 *            The address to use when constructing the codebase
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param sdbConfig
	 *            The configuration options the DatabaseStore will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>sdb-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getDatabaseStorer(String policy,
			String hostAddress, int port, String... sdbConfig)
			throws IOException {
		if (sorcerHome == null)
			throw new RuntimeException("'sorcer.home' property not declared");
		
		// service provider classpath
		String dbpc = Resolver.resolveClassPath(
                //SOS_PLATFORM,
				DBP_PRV,
				SLEEPYCAT,
				SOS_PLATFORM
		);
		
		// service provider codebase
        String dbpCodebase = getCodebase(new ArtifactCoordinates[]{
				SOS_PLATFORM,
				//SOS_ENV,
				SERVICEUI,
				EXERTLET_UI,
		}, hostAddress, Integer.toString(port));
		
		String implClass = "sorcer.core.provider.dbp.DatabaseProvider";
		return (new SorcerServiceDescriptor(dbpCodebase, policy,
				dbpc, implClass, sdbConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.service.DataspaceStorer} using the Webster port created
	 * by this utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param sdbConfig
	 *            The configuration options the DataspaceStore will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>sdb-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getDataspaceStorer(String policy,
			String... sdbConfig) throws IOException {
		return (getDataspaceStorer(policy, Booter.getPort(), sdbConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.service.DataspaceStorer}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param sdbConfig
	 *            The configuration options the DataspaceStore will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>sdb-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getDataspaceStorer(String policy, int port,
			String... sdbConfig) throws IOException {
		return (getDataspaceStorer(policy, Booter.getHostAddress(), port, sdbConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.service.DataspaceStorer}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param hostAddress
	 *            The address to use when constructing the codebase
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param sdbConfig
	 *            The configuration options the DataspaceStore will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Monitor using an anonymous port. The <tt>sdb-prv.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getDataspaceStorer(String policy,
			String hostAddress, int port, String... sdbConfig)
			throws IOException {
		if (sorcerHome == null)
			throw new RuntimeException("'sorcer.home' property not declared");
		
		// service provider classpath
		String dbpc = Resolver.resolveClassPath(DSP_PRV, SLEEPYCAT, SOS_PLATFORM);
		
		// service provider codebase
        String dbpCodebase = getCodebase(new ArtifactCoordinates[]{
				SOS_PLATFORM,
				//SOS_ENV,
				SERVICEUI,
				EXERTLET_UI,
		}, hostAddress, Integer.toString(port));
		
		String implClass = "sorcer.core.provider.dsp.DataspaceProvider";
		return (new SorcerServiceDescriptor(dbpCodebase, policy,
				dbpc, implClass, sdbConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.Cataloger} using the Webster port created
	 * by this utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param catalogerConfig
	 *            The configuration options the Cataloger will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Cataloger using an anonymous port. The <tt>cataloger.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getCataloger(String policy,
			String... catalogerConfig) throws IOException {
		return (getCataloger(policy, Booter.getPort(), catalogerConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.Cataloger}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param catalogerConfig
	 *            The configuration options the Cataloger will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Cataloger using an anonymous port. The <tt>cataloger.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getCataloger(String policy, int port,
			String... catalogerConfig) throws IOException {
		return (getCataloger(policy, Booter.getHostAddress(), port, catalogerConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.Cataloger}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param hostAddress
	 *            The address to use when constructing the codebase
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param catalogerConfig
	 *            The configuration options the Cataloger will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         Cataloger using an anonymous port. The <tt>cataloger.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getCataloger(String policy,
			String hostAddress, int port, String... catalogerConfig)
			throws IOException {
		if (sorcerHome == null)
			throw new RuntimeException("'sorcer.home' property not declared");
		
		// service provider classpath
		String catalogClasspath = Resolver.resolveClassPath(CATALOGER_PRV, SOS_PLATFORM);

		// service provider codebase		
		String catalogCodebase = getCodebase(new ArtifactCoordinates[]{
			    SOS_PLATFORM,
				//SOS_ENV,
				SERVICEUI,
				EXERTLET_UI,
		}, hostAddress, Integer.toString(port));
		
		String implClass = "sorcer.core.provider.cataloger.ServiceCataloger";
		return (new SorcerServiceDescriptor(catalogCodebase, policy,
				catalogClasspath, implClass, catalogerConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.logger.ServiceLogger} using the Webster port created
	 * by this utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param loggerConfig
	 *            The configuration options the ServiceLogger will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         ServiceLogger using an anonymous port. The <tt>logger.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getLogger(String policy,
			String... loggerConfig) throws IOException {
		return (getLogger(policy, Booter.getPort(), loggerConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.logger.RemoteLoggerManager}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param loggerConfig
	 *            The configuration options the ServiceLogger will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         ServiceLogger using an anonymous port. The <tt>logger.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getLogger(String policy, int port,
			String... loggerConfig) throws IOException {
		return (getLogger(policy, Booter.getHostAddress(), port, loggerConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * {@link sorcer.core.provider.logger.RemoteLoggerManager}.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param hostAddress
	 *            The address to use when constructing the codebase
	 * @param port
	 *            The port to use when constructing the codebase
	 * @param loggerConfig
	 *            The configuration options the ServiceLogger will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 *         ServiceLogger using an anonymous port. The <tt>logger.jar</tt> file
	 *         will be loaded from <tt>sorcer.home/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>sorcer.home</tt> system property is not set
	 */
	public static ServiceDescriptor getLogger(String policy,
			String hostAddress, int port, String... loggerConfig)
			throws IOException {
		if (sorcerHome == null)
			throw new RuntimeException("'sorcer.home' property not declared");
		
		// service provider classpath
		String loggerClasspath = Resolver.resolveClassPath(LOGGER_PRV, LOGGER_SUI, SOS_PLATFORM);

		// service provider codebase
		String loggerCodebase = getCodebase(new ArtifactCoordinates[]{
				SOS_PLATFORM,
				//SOS_ENV,
				SERVICEUI,
				EXERTLET_UI,
				LOGGER_SUI,
		}, hostAddress, Integer.toString(port));
		// Logger is a partner to ServiceTasker
		String implClass = "sorcer.core.provider.logger.RemoteLoggerManager";
		return (new SorcerServiceDescriptor(loggerCodebase, policy,
				loggerClasspath, implClass, loggerConfig));

	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 * Jini Lookup Service (Reggie), using the Webster port created by this
	 * utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param lookupConfig
	 *            The configuration file Reggie will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
	 *         Reggie using an anonymous port. The <tt>reggie.jar</tt> file will
	 *         be loaded from <tt>JINI_HOME/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>JINI_HOME</tt> system property is not set
	 */
	public static ServiceDescriptor getLookup(String policy,
			String... lookupConfig) throws IOException {
		return (getLookup(policy, Booter.getPort(), lookupConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 * Jini Lookup Service, Reggie.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @param lookupConfig
	 *            The configuration options Reggie will use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * @param port
	 *            The port to use when constructing the codebase Reggie using an
	 *            anonymous port. The <tt>reggie.jar</tt> file will be loaded
	 *            from <tt>JINI_HOME/lib</tt>
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>JINI_HOME</tt> system property is not set
	 */
	public static ServiceDescriptor getLookup(String policy, int port,
			String... lookupConfig) throws IOException {
		return (getLookup(policy, Booter.getHostAddress(), port, lookupConfig));
	}

	/**
	 * Get the {@link com.sun.jini.start.ServiceDescriptor} instance for the
	 * Jini Lookup Service (Reggie), using the Webster port created by this
	 * utility.
	 * 
	 * @param policy
	 *            The security policy file to use
	 * @return The {@link com.sun.jini.start.ServiceDescriptor} instance for
	 * @param hostAddress
	 *            The address to use when constructing the codebase
	 * @param port
	 *            The port to use when constructing the codebase Reggie using an
	 *            anonymous port. The <tt>reggie.jar</tt> file will be loaded
	 *            from <tt>JINI_HOME/lib</tt>
	 * @param lookupConfig
	 *            The configuration options Reggie will use
	 * 
	 * @throws IOException
	 *             If there are problems getting the anonymous port
	 * @throws RuntimeException
	 *             If the <tt>JINI_HOME</tt> system property is not set
	 */
	public static ServiceDescriptor getLookup(String policy,
			String hostAddress, int port, String... lookupConfig)
			throws IOException {
		if (sorcerHome == null)
			throw new RuntimeException("'sorcer.home' system property not declared");
		String reggieClasspath = Resolver.resolveClassPath(
				coords("org.apache.river:reggie"));
		String reggieCodebase = getCodebase(new ArtifactCoordinates[]{
				coords("org.apache.river:reggie-dl")
		}, hostAddress, Integer.toString(port));
 		String implClass = "com.sun.jini.reggie.TransientRegistrarImpl";
		return (new SorcerServiceDescriptor(reggieCodebase, policy,
				reggieClasspath, implClass, lookupConfig));
	}
	
	protected static String[] getArray(String s, String[] array) {
		String[] sArray;
		if (array != null && array.length > 0) {
			sArray = new String[array.length + 1];
			sArray[0] = s;
			System.arraycopy(array, 0, sArray, 1, sArray.length - 1);
		} else {
			sArray = new String[] { s };
		}
		return (sArray);
	}

	public static String getCodebase(ArtifactCoordinates[] artifacts, String address, String port) {
		String[] jars = new String[artifacts.length];
		for (int i = 0; i < artifacts.length; i++) {
			jars[i] = Resolver.resolveRelative(artifacts[i]);
		}
		return Booter.getCodebase(jars, address, port);
	}
}
