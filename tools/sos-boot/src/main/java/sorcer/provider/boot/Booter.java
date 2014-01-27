/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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
package sorcer.provider.boot;

import org.apache.commons.lang3.StringUtils;
import org.rioproject.resolver.Artifact;
import org.rioproject.resolver.RemoteRepository;
import org.rioproject.resolver.ResolverException;
import org.rioproject.resolver.ResolverHelper;
import sorcer.core.SorcerConstants;
import sorcer.core.SorcerEnv;
import sorcer.resolver.Resolver;
import sorcer.resolver.VersionResolver;
import sorcer.util.ArtifactCoordinates;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static sorcer.core.SorcerConstants.*;

/**
 * Provides static convenience methods for use in configuration files. This class cannot
 * be instantiated.
 *
 * @author Dennis Reedy and Mike Sobolewski
 */
public class Booter {
	private static final String COMPONENT = "sorcer.provider.boot";
	private static Logger logger = Logger.getLogger(COMPONENT);

	/**
	 * Code server (wester) port
	 */
	private static int port = -1;
	
	/**
	 * Default name 'provider.properties' for a file defining provider
	 * properties.
	 */
	public static String PROVIDER_PROPERTIES_FILENAME = "provider.properties";

	/**
	 * Default name 'data.formats' for a file defining service context node
	 * types.
	 */
	private static String CONTEXT_DATA_FORMATS = "data.formats";

	/**
	 * All the properties from sorcer.env.
	 */
	private static Properties props = new Properties();

    private static final VersionResolver versionResolver = new VersionResolver();
    private static final org.rioproject.resolver.Resolver resolver;

    private static final String localRepo;

	static {
		try {
            resolver = ResolverHelper.getResolver();
        } catch (ResolverException e) {
            throw new IllegalStateException("could not initialize Aether resolver", e);
        }

		try {
			loadEnvironment();
		} catch (IOException e) {
			logger.severe("Cannot load the SORCER environment");
		}
        try {
            localRepo = new File(getLocalRepoRootUrl().toURI()).getPath();
        } catch (URISyntaxException e) {
            throw new ExceptionInInitializerError(e);
        }

        SorcerEnv.setBootable(true);
    }

	/** This class cannot be instantiated. */
	private Booter() {
		throw new AssertionError(
				"sorcer.provider.boot.BootUtil cannot be instantiated");
	}

    /**
     * API for configs
     * resolve codebase from artifact coordinates with maven
     */
    public static String resolveCodebase2(String coords) throws ResolverException, URISyntaxException, MalformedURLException {
        if (!Artifact.isArtifact(coords)) {
            int pos = coords.indexOf(':');
            if (pos < 1)
                throw new IllegalArgumentException("Invalid artifact " + coords);
            String groupdId = coords.substring(0, pos);
            String artifactId = coords.substring(pos + 1);
            String ver = versionResolver.resolveVersion(groupdId, artifactId);
            coords = coords + ":" + ver;
        }

        return resolveCodebase2(ArtifactCoordinates.coords(coords));
    }

    public static String resolveCodebase2(ArtifactCoordinates artifact) throws ResolverException, URISyntaxException, MalformedURLException {
        String[] resolved = ResolverHelper.resolve(artifact.toString(), resolver, new RemoteRepository[0]);
        URI codeBaseRoot = SorcerEnv.getCodebaseRoot().toURI();

        for (int i = 0; i < resolved.length; i++) {
            String path = new File(new URL(resolved[i]).toURI()).getPath();
            String relative = path.substring(localRepo.length()).replace("\\", "/");
            resolved[i] = codeBaseRoot.resolve(relative).toString();
        }
        return StringUtils.join(resolved, SorcerConstants.CODEBASE_SEPARATOR);
    }

    private static URL getLocalRepoRootUrl() {
        try {
            return new File(SorcerEnv.getRepoDir()).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * API for configs
	 * resolve classpath from artifact coordinates
	 */
	public static String resolveClasspath(String coords){
		return Resolver.resolveAbsolute(coords);
	}

	/**
	 * API for configs
	 * resolve classpath from artifact coordinates
	 */
	public static String resolveClasspath(String[] coords){
		return Resolver.resolveClassPath(coords);
	}

	/**
	 * API for configs
	 * resolve codebase from artifact coordinates
	 */
	public static String resolveCodebase(String[] coords) throws UnknownHostException, MalformedURLException {
		return Resolver.resolveCodeBase(SorcerEnv.getCodebaseRoot(), coords);
	}

	/**
	 * API for configs
	 * resolve codebase from artifact coordinates
	 */
	public static String resolveCodebase(String coords) throws UnknownHostException, MalformedURLException {
        String[] coordsEntries = coords.split(" ");
		return resolveCodebase(coordsEntries);
	}

    /**
     * Return the classpath for the provided JAR names. 
     * 
     * @param jars Array of JAR names
     * @return The classpath with system dependent path delimiters 
     */
    public static String getClasspath(String[] jars) {
        StringBuffer buffer = new StringBuffer();
        for(int i=0; i<jars.length; i++) {
            if(i>0)
                buffer.append(File.pathSeparator);
            buffer.append(jars[i]);
        }
        return(buffer.toString());        
    }

	/**
     * Return the codebase for the provided JAR name and port. This method will 
     * first get the IP Address of the machine using 
     * <code>java.net.InetAddress.getLocalHost().getHostAddress()</code>, then
     * construct the codebase using the host address and the port for the provided 
     * jar.
     * 
     * @param jar The JAR to use
     * @param port The port to use when constructing the codebase
     * @return the codebase for the JAR
     * @throws java.net.UnknownHostException if no IP address for the local
     * host could be found.
     */
    public static String getCodebase(String jar, String port) 
    throws java.net.UnknownHostException {
        return(getCodebase(jar, getHostAddress(), port));        
    }
    
    /**
     * Return the codebase for the provided JAR names and port. This method will 
     * first get the IP Address of the machine using 
     * <code>java.net.InetAddress.getLocalHost().getHostAddress()</code>, then 
     * construct the codebase using the host address and the port for each jar in 
     * the array of jars.
     * 
     * @param jars The JAR names to use
     * @param port The port to use when constructing the codebase
     * @return The codebase as a space-delimited String for the provided JARs
     * @throws java.net.UnknownHostException if no IP address for the local
     * host could be found.
     */
    public static String getCodebase(String[] jars, String port) 
    throws java.net.UnknownHostException {
        return(getCodebase(jars, getHostAddress(), port));        
    }
    
    /**
     * Return the codebase for the provided JAR name, port and address
     * 
     * @param jar The JAR to use
     * @param address The address to use when constructing the codebase
     * @param port The port to use when constructing the codebase
     * @return the codebase for the JAR
     */
    public static String getCodebase(String jar, String address, String port) {
		try {
			return new URL(getCodebaseRoot(address, Integer.parseInt(port)), jar).toExternalForm();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
     * Return the codebase for the provided JAR names, port and address
     * 
     * @param jars Array of JAR names
     * @param address The address to use when constructing the codebase
     * @param port The port to use when constructing the codebase
     * @return the codebase for the JAR
     */
    public static String getCodebase(String[] jars, String address, String port) {
		int p = new Integer(port);
		if (p <= 0)
			Booter.port = getPort();

		URL baseUrl = getCodebaseRoot(address, Booter.port);
		return SorcerEnv.getCodebase(baseUrl, jars);
	}

    protected static URL getCodebaseRoot(String host, int port) {
		return getCodebaseRoot("http", host, port);
	}

	protected static URL getCodebaseRoot(String schema, String host, int port){
		try {
			return new URL(schema, host, port, "");
		} catch (MalformedURLException e) {
			throw new RuntimeException("Could not build base URL", e);
		}
	}

    /**
     * Return the local host address using 
     * <code>java.net.InetAddress.getLocalHost().getHostAddress()</code>
     * 
     * @return The local host address
     * @throws java.net.UnknownHostException if no IP address for the local
     * host could be found.
     */
    public static String getHostAddress() throws java.net.UnknownHostException {
		return SorcerEnv.getLocalHost().getHostAddress();
    }

    /**
     * Return the local host address for a passed in host using
     * {@link java.net.InetAddress#getByName(String)}
     *
     * @param name The name of the host to return
     *
     * @return The local host address
     *
     * @throws java.net.UnknownHostException if no IP address for the host name
     * could be found.
     */
    public static String getHostAddress(String name)
    throws java.net.UnknownHostException {
        return java.net.InetAddress.getByName(name).getHostAddress();
    }

    /**
     * Return the local host address based on the value of a system property.
     * using {@link java.net.InetAddress#getByName(String)}. If the system
     * property is not resolvable, return the default host address obtained from
     * {@link java.net.InetAddress#getLocalHost()}
     *
     * @param property The property name to use
     *
     * @return The local host address
     *
     * @throws java.net.UnknownHostException if no IP address for the host name
     * could be found.
     */
    public static String getHostAddressFromProperty(String property)
        throws java.net.UnknownHostException {
        String host = getHostAddress();
        String value = System.getProperty(property);
        if(value != null) {
            host = java.net.InetAddress.getByName(value).getHostAddress();
        }
        return(host);
    }
    
    /**
     * Return the local host name 
     * <code>java.net.InetAddress.getLocalHost().getHostName()</code>
     * 
     * @return The local host name
     * @throws java.net.UnknownHostException if no hostname for the local
     * host could be found.
     */
    public static String getHostName() throws java.net.UnknownHostException {
        return SorcerEnv.getLocalHost().getHostName();
    }
    
    /**
     * Get an URL from a fully qualified file path
     * 
     * @param filePath A fully qualified file path
     * 
     * @return The URL from the file if the file exists
     * @throws MalformedURLException If the filePath cannot be converted
     */
    public static URL fileToURL(String filePath) throws MalformedURLException {
        File file = new File(filePath);
        return(file.toURI().toURL());
    }
    
    /**
     * Will return an array of URLs based on the input String array. If the array 
     * element is a file, the fully qualified file path must be provided. If the
     * array element is a directory, a fully qualified directory path must be 
     * provided, and the directory will be searched for all .jar and .zip files
     * 
     * @param elements A String array of fully qualified directory path
     * 
     * @return An URL[] elements
     * 
     * @throws MalformedURLException if any of the elements cannot be converted
     */
    public static URL[] toURLs(String[] elements)throws MalformedURLException {
        ArrayList<URL> list = new ArrayList<URL>();
        if(elements!=null) {
            for (String el : elements) {
                File element = new File(el);
                if (element.isDirectory()) {
                    URL[] urls = scanDirectory(el);
                    list.addAll(Arrays.asList(urls));
                } else {
                    list.add(element.toURI().toURL());
                }
            }
        }
        return(list.toArray(new URL[list.size()]));
    }
    
    /**
     * Will return an array of URLs for all .jar and .zip files in the directory,
     * including the directory itself
     * 
     * @param dirPath A fully qualified directory path
     * 
     * @return A URL[] for all .jar and .zip files in the directory,
     *  including the directory itself
     * 
     * @throws MalformedURLException If elements in the directory cannot be
     * created into a URL
     */
    public static URL[] scanDirectory(String dirPath) throws MalformedURLException {
        File dir = new File(dirPath);
        if(!dir.isDirectory())
            throw new IllegalArgumentException(dirPath+" is not a directory");
        if(!dir.canRead())
            throw new IllegalArgumentException("No read permissions for "+dirPath);
        File[] files = dir.listFiles();
        ArrayList<URL> list = new ArrayList<URL>();
        
        list.add(dir.toURI().toURL());

        for (File file : files) {
            if (file.getName().endsWith(".jar") ||
                file.getName().endsWith(".JAR") ||
                file.getName().endsWith(".zip") ||
                file.getName().endsWith(".ZIP")) {

                if (file.isFile())
                    list.add(file.toURI().toURL());
            }
        }
        return(list.toArray(new URL[list.size()]));
    }
    
    /**
     * Convert a comma, space and/or {@link java.io.File#pathSeparator} 
     * delimited String to array of Strings
     *
     * @param arg The String to convert
     *
     * @return An array of Strings
     */    
    public static String[] toArray(String arg) {
        StringTokenizer tok = new StringTokenizer(arg, " ,"+File.pathSeparator);
        String[] array = new String[tok.countTokens()];
        int i=0;
        while(tok.hasMoreTokens()) {            
            array[i] = tok.nextToken();
            i++;
        }
        return(array);
    }

    /**
     * Read the properties file and return an array of String instances which
     * can be used as Jini configuration overrides
     *
     * @param propertiesFile A Properties file containing override declarations
     *
     * @return An array of String values suitable for use as Jini configuration
     * overrides. If the propeties file is empty return a zero-length array
     *
     * @throws NullPointerException if propertiesFile is null
     * @throws IOException if the Properties file cannot be read
     */
    public static String[] parseOverrides(String propertiesFile) throws
                                                               IOException {
        return(parseOverrides(propertiesFile, null));
    }

    /**
     * Read the properties file and return an array of String instances which
     * can be used as Jini configuration overrides
     *
     * @param propertiesFile A Properties file containing override declarations
     * @param components An array of components to match, maty be null. If not
     * null, then if a key starts with an element in the provided components
     * array it will be part of the array of overrides returned. If this
     * parameter is null or zero-length, all property components are returned
     *
     * @return An array of String values suitable for use as Jini configuration
     * overrides. If there are no matching components or the properties file is
     * empty return a zero-length array
     *
     * @throws NullPointerException if propertiesFile is null
     * @throws IOException if the Properties file cannot be read
     */
    public static String[] parseOverrides(String propertiesFile,
                                          String[] components) throws
                                                               IOException {
        if(propertiesFile==null)
            throw new NullPointerException("propertiesFile is null");
        ArrayList<String> list = new ArrayList<String>();
        Properties props = new Properties();
        props.load(new FileInputStream(propertiesFile));
        if(components!=null && components.length>0) {
            for(Enumeration e=props.propertyNames(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                for (String component : components) {
                    if (key.startsWith(component)) {
                        list.add(key + "=" + props.getProperty(key));
                    }
                }
            }
        } else {
            for(Enumeration e=props.propertyNames(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                list.add(key+"="+props.getProperty(key));
            }
        }
        return(list.toArray(new String[list.size()]));
    }

    /**
     * Assemble a String array suitable for use as Jini configuration options.
     * The String array whose first element is the configuration source and
     * remaining elements specify override values for entries
     *
     * @param configFile The location of the configuration file, must not be
     * null
     * @param overrides Array of elements specifying override values for
     * entries, mat be null
     *
     * @return A String array suitable for use as a Jini configuration
     *
     * @throws NullPointerException if configFile is null
     */
    public static String[] createConfigArgs(String configFile, String[] overrides) {
        String[] configArgs;
        if(overrides!=null && overrides.length>0) {
            configArgs = new String[overrides.length+1];
            configArgs[0] = configFile;
            System.arraycopy(overrides, 0, configArgs, 1, overrides.length);
        } else {
            configArgs = new String[]{configFile};
        }
        return(configArgs);
    }
    
    
    /**
	 * Tries to load properties from a <code>filename</code>, first in a local
	 * directory. If there is no file in the local directory then load the file
	 * from the classpath at sorcer/util/sorcer.env.
     * @throws IOException 
	 * 
	 * @throws IOException
	 */
	private static void loadEnvironment() throws IOException {
		String envFile = System.getProperty("sorcer.env.file");
		try {
			// check if the properties file is specified by the system property
			if (envFile != null) {
				props.load(new FileInputStream(envFile));
			} else {
				envFile = "sorcer.env";

				// Try in user home directory first
				props.load((new FileInputStream(new File(envFile))));
				logger.info("got Env properties in user home: "
						+ System.getProperty("user.home"));
			}
		} catch (Throwable t1) {
			try {
				// No file at home, give a try as resource
				// sorcer/util/sorcer.env
				//
				InputStream stream = SorcerConstants.class.getResourceAsStream(envFile);
				if (stream != null)
					props.load(stream);
				else
					logger.severe("could not load the SORCER Env properties");
			} catch (Throwable t2) {
				throw new IOException();
			}
		}
		
		// use the local host address if requested
		String pattern = "${localhost}";
        String value = props.getProperty("provider.webster.interface");
        if (value!=null && value.indexOf(pattern) >= 0) {
			try {
				String val = value.replace(pattern, getHostAddress());
				props.put("provider.webster.interface", val);
			} catch (UnknownHostException e1) {
                logger.log(Level.WARNING, "Could not resolve hostname", e1);
            }
		}
	}
	
	/**
	 * Gets the value of a certain property.
	 * 
	 * @param property
	 * @return the string value of that property
	 */
	public static String getProperty(String property) {
		String p = props.getProperty(property);
		if (p == null) {
			logger.finer("SORCER Env property not set: " + property);
		}
		return p;
	}

	/**
	 * Gets the value for a certain property or the default value if property is
	 * not set.
	 * 
	 * @param property
	 * @param defaultValue
	 * @return the string value of that property
	 */
	public static String getProperty(String property, String defaultValue) {
		return props.getProperty(property, defaultValue);
	}

	/**
	 * Return the interface of a SORCER class server.
	 * @return a webster network interface address
	 */
	public static String getWebsterInterface()  {
		String intf = System.getenv("WEBSTER_INTERFACE");

		if (intf  != null && intf.length() > 0) {
			logger.finer("webster interface as the system environment value: "+ intf);
			return intf;
		}

		intf = System.getProperty(P_WEBSTER_INTERFACE);
		if (intf != null && intf.length() > 0) {
			logger.finer("webster interface as 'provider.webster.interface' property value: "+ intf);
			return intf;
		}

		intf = props.getProperty(P_WEBSTER_INTERFACE);
		if (intf != null && intf.length() > 0)
		{
			logger.finer("webster interface as 'provider.webster.interface' property value: "+ intf);
			return intf;
		}
		try 
		{
			logger.finer("webster interface  as the local host value: " + intf);
			intf = Booter.getHostAddress();
		} 
		catch (UnknownHostException e) 
		{
			logger.severe("Cannot determine the webster interface.");
			e.printStackTrace();
		}          
		return intf;
	}
	
	/**
	 * Checks which port to use for a SORCER class server.
	 * 
	 * @return a port number
	 */
	public static int getWebsterPort() {
		String wp = System.getenv("WEBSTER_PORT");
		if (wp != null && wp.length() > 0) {
//			logger.info("webster port as 'WEBSTER_PORT': " + wp);
			return new Integer(wp);
		}

		wp = System.getProperty(S_WEBSTER_PORT);
		if (wp != null && wp.length() > 0) {
//			logger.info("webster port as System 'webster.port': " + wp);
			return new Integer(wp);
		}

		wp = props.getProperty(P_WEBSTER_PORT);
		if (wp != null && wp.length() > 0) {
//			logger.finer("webster port as in sorcer.env 'provider.webster.port': " + wp);
			return new Integer(wp);
		}

		return 0;
	}
	
	/**
	 * Returns the start port to use for a SORCER code server.
	 * 
	 * @return a port number
	 */
	public static int getWebsterStartPort() {
		String hp = System.getenv("WEBSTER_START_PORT");

		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		hp = System.getProperty(S_WEBSTER_START_PORT);
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}
		
		hp = props.getProperty(P_WEBSTER_START_PORT);
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		return 0;
	}
	
	/**
	 * Returns the end port to use for a SORCER code server.
	 * 
	 * @return a port number
	 */
	public static int getWebsterEndPort() {
		String hp = System.getenv("WEBSTER_END_PORT");

		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		hp = System.getProperty(S_WEBSTER_END_PORT);
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}
		
		hp = props.getProperty(P_WEBSTER_END_PORT);
		if (hp != null && hp.length() > 0) {
			return new Integer(hp);
		}

		return 0;
	}
	
	/**
	 * Returns the webster port of the iGrid environment. If the the returned
	 * port is anonymous then the system property "provider.webster.port" is
	 * assigned to the returned port.
	 * 
	 * @return a webster port
	 */
	static int getPort() {
		if (port < 0) {
			port = getWebsterPort();
			if (port > 0)
				return port;
		}

		if (port == 0) {
			try {
				port = Booter.getAnonymousPort();
				System.setProperty("webster.port", "" + port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return port;
	}
	 
	   /**
     * Get an anonymous port
     * 
     * @return An anonymous port created by instantiating a 
     * <code>java.net.ServerSocket</code> with a port of 0
     *
     * @throws IOException If an anonymous port cannot be obtained
     */
    public static int getAnonymousPort() throws java.io.IOException {
        java.net.ServerSocket socket = new java.net.ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    private static List<String> websterRoots = Arrays.asList(
            SorcerEnv.getRepoDir(),
            SorcerEnv.getLibPath(),
            new File(SorcerEnv.getHomeDir(), "deploy").getPath()
    );

    static {
        String scratch = System.getProperty(SCRATCH_DIR);
        if (scratch != null) {
            List<String> oldWr = websterRoots;
            websterRoots = new ArrayList<String>(oldWr.size() + 1);
            websterRoots.addAll(oldWr);
            websterRoots.add(scratch);
        }
    }

    public static String[] getWebsterRoots() {
        return websterRoots.toArray(new String[websterRoots.size()]);
    }

    public static String[] getWebsterRoots(String[] additional) {
        String[] result = new String[websterRoots.size() + additional.length];
        addAll(websterRoots, result);
        System.arraycopy(additional, 0, result, websterRoots.size(), additional.length);
        return result;
    }

    private static <T> void addAll(List<T> src, T[] target) {
        int i = 0;
        for (T o : src) {
            target[i] = o;
            ++i;
        }
    }
}
