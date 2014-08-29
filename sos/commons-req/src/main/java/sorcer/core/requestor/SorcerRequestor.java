/*
 * Copyright 2010 the original author or authors.
 * Copyright 2010 SorcerSoft.org.
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

package sorcer.core.requestor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.RMISecurityManager;
import java.util.Arrays;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import sorcer.core.SorcerConstants;
import sorcer.service.ConfigurationException;
import sorcer.tools.webster.InternalWebster;
import sorcer.util.Sorcer;
import sorcer.util.SorcerUtil;

/**
 * This an abstract class with the abstract methods that defines the initialization
 * of SORCER requestor. This class implements {@link SorcerConstants}. The 
 * main method of the class initializes system properties. Then it parses the 
 * arguments passed to method if no arguments are passed to the method it 
 * terminates the Java Virtual Machine with an exit code of 1. Else, the first 
 * argument is set as the runnerType. Then the code attempts to create a
 * new instance of the requestor as service runner if it fails the Virtual 
 * Machine exits with a code of 2. Next the code determines if an internal 
 * Webster is running. If one is running it attempts to get the webster 
 * roots paths. Finally the method call the {@code run} function. 
 *  
 * @author M. W. Sobolewski
 * @see SorcerConstants
 */
abstract public class SorcerRequestor implements SorcerConstants {
	/** Logger for logging information about this instance */
	protected static final Logger logger = Logger
			.getLogger(SorcerRequestor.class.getName());

	public static String R_PROPERTIES_FILENAME = "requestor.properties";
	protected static SorcerRequestor requestor = null;
	protected Properties props = new Properties();
	protected int port;

	/**
	 * Main method for the SorcerRequestor class
	 * @param args String array containing arguments for the main method
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception {
		// Setup the security manager
		System.setSecurityManager(new RMISecurityManager());

		// Initialize system properties: configs/sorcer.env
		Sorcer.getEnvProperties();
		
		// Determine runner type from argument array 
		String runnerType = null;
		if (args.length == 0) {
			System.err
					.println("Usage: Java sorcer.core.requestor.SorcerRequestor <runnerType>");
			System.exit(1);
		} else {
			runnerType = args[0];
		}
		
		// Establish a new instance of the class runner
		try {
			requestor = (SorcerRequestor) Class.forName(runnerType)
					.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("Not able to create service runner: " + runnerType);
			System.exit(2);
		}
		
		// Attempt to load the requestor properties file
		String str = System.getProperty(R_PROPERTIES_FILENAME);
		logger.info(R_PROPERTIES_FILENAME + " = " + str);
		if (str != null) {
			requestor.loadProperties(str); // search the provider package
		} else {
			throw new RuntimeException("No requestor properties file available!");
		}
		
		// Determine if an internal web server is running if so obtain the root paths
		boolean isWebsterInt = false;
		String val = System.getProperty(SORCER_WEBSTER_INTERNAL);
		if (val != null && val.length() != 0) {
			isWebsterInt = val.equals("true");
		}
		if (isWebsterInt) {
			String roots = System.getProperty(SorcerConstants.WEBSTER_ROOTS);
			String[] tokens = null;
			if (roots != null)
				tokens = toArray(roots);
			try {
                if (tokens!=null || System.getProperty(CODEBASE_JARS)!=null)
				    InternalWebster.startWebster(tokens);
                else
                    logger.warning("Not starting internal webster because no \"codebase.jars\" are specified");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
			
		// Initialize the requestor
		requestor.run(args);
	}
	
	/**
	 * This is an abstract method that runs the SorcerRequestor
	 * @param args String array containing arguments passed to the main method
	 * @throws Exception
	 */
	abstract public void run(String... args) throws Exception;
	
	
	/**
	 * Loads service requestor properties from a <code>filename</code> file. 
	 * 
	 * @param filenames
	 *            the properties file name see #getProperty
	 * @throws ConfigurationException 
	 */
	public void loadProperties(String... filenames) throws ConfigurationException {
		logger.info("loading requestor properties:" + Arrays.toString(filenames));
        for (String fName : filenames)
            props.putAll(Sorcer.loadProperties(fName));
	}

	public String getProperty(String key) {
		return props.getProperty(key);
	}

	public Object setProperty(String key, String value) {
		return props.setProperty(key, value);
	}
	
	public String getProperty(String property, String defaultValue) {
		return props.getProperty(property, defaultValue);
	}
	
	/**
	 * Returns a URL for the requestor's data server.
	 * 
	 * @return the current URL for the requestor's data server.
	 */
	public String getDataServerUrl() {
		return "http://" + getProperty(DATA_SERVER_INTERFACE) + ':' + getProperty(DATA_SERVER_PORT);
	}

	/**
	 * Returns the hostname of a requestor data server.
	 * 
	 * @return a data server name.
	 */
	public String getDataServerInterface() {
		return  System.getProperty(R_DATA_SERVER_INTERFACE);
		}
	

	/**
	 * Returns the port of a requestor data server.
	 * 
	 * @return a data server port.
	 */
	public String getDataServerPort() {
		return  System.getProperty(R_DATA_SERVER_PORT);
		}
	
	/**
	 * Returns a URL for the SORCER class server.
	 * 
	 * @return the current URL for the SORCER class server.
	 */
	public String getWebsterUrl() {
		return "http://" + getWebsterInterface() + ':' + getWebsterPort();
	}

	/**
	 * Returns the hostname of a requestor class server.
	 * 
	 * @return a webster host name.
	 */
	public String getWebsterInterface() {
		String hn = System.getenv("IGRID_WEBSTER_INTERFACE");

		if (hn != null && hn.length() > 0) {
			logger.finer("webster hostname as the system environment value: "
					+ hn);
			return hn;
		}

		hn = System.getProperty(R_WEBSTER_INTERFACE);
		if (hn != null && hn.length() > 0) {
			logger
					.finer("webster hostname as '" + R_WEBSTER_INTERFACE + "' system property value: "
							+ hn);
			return hn;
		}

		hn = props.getProperty(R_WEBSTER_INTERFACE);
		if (hn != null && hn.length() > 0) {
			logger
					.finer("webster hostname as '" + R_WEBSTER_INTERFACE + "' provider property value: "
							+ hn);
			return hn;
		}

		try {
			hn = Sorcer.getHostName();
			logger.finer("webster hostname as the local host value: " + hn);
		} catch (UnknownHostException e) {
			logger.severe("Cannot determine the webster hostname.");
		}

		return hn;
	}
	
	/**
	 * Checks which port to use for a requestor class server.
	 * 
	 * @return a port number
	 */
	public int getWebsterPort() {
		if (port != 0)
			return port;

		String wp = System.getenv("IGRID_WEBSTER_PORT");
		if (wp != null && wp.length() > 0) {
			logger.finer("requestor webster port as 'IGRID_WEBSTER_PORT': " + wp);
			return new Integer(wp);
		}

		wp = System.getProperty(R_WEBSTER_PORT);
		if (wp != null && wp.length() > 0) {
			logger.finer("requestor webster port as System '" + R_WEBSTER_PORT + "': "
					+ wp);
			return new Integer(wp);
		}

		wp = props.getProperty(R_WEBSTER_PORT);
		if (wp != null && wp.length() > 0) {
			logger.finer("requestor webster port as Sorcer '" + R_WEBSTER_PORT + "': "
					+ wp);
			return new Integer(wp);
		}

		try {
			port = Sorcer.getAnonymousPort();
			logger.finer("anonymous requestor webster port: " + wp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return port;
	}

	/**
	 * Returns the URL for the requestor's <code>filename</code>
	 * 
	 * @return the current URL for the SORCER requestor data server.
	 * @throws MalformedURLException 
	 */
	public URL getRequestorDataFileURL(String filename) throws MalformedURLException {
		return new URL("http://" + getDataServerUrl() + '/'
				+ getProperty(R_DATA_DIR) + '/' + filename);
	}

	public File getScrachFile(String filename) {
		return new File(getNewScratchDir() + File.separator + filename);
	}

	/**
	 * Returns a directory for requestor's scratch files
	 * 
	 * @return a scratch directory
	 */
	 public File getScratchDir() {
		 return Sorcer.getNewScratchDir();
	}
	
	/**
	 * Deletes a direcory and all its files.
	 * 
	 * @param dir
	 *            to be deleted
	 * @return true if the directory is deleted
	 * @throws Exception
	 */
	public boolean deleteDir(File dir) throws Exception {
		return SorcerUtil.deleteDir(dir);
	}
		
	/**
	 * Returns a directory for requestor's scratch files
	 * 
	 * @return a scratch directory
	 */
	public File getNewScratchDir() {
		return Sorcer.getNewScratchDir();
	}

	public File getDataFile(String filename) {
		return new File(getDataDir() + File.separator + filename);
	}
	
	/**
	 * Returns a directory for requestor's data root.
	 * 
	 * @return a requestor data root directory
	 */
	public File getDataRootDir() {
		return new File(getProperty(R_DATA_ROOT_DIR));
	}
	
	/**
	 * Returns a directory for requestor's data.
	 * 
	 * @return a requestor data directory
	 */
	public File getDataDir() {
		//return new File(getProperty(R_DATA_ROOT_DIR) + File.separator + getProperty(R_DATA_DIR));
		return new File(System.getProperty(DOC_ROOT_DIR));
	}

	/**
	 * Returns the URL for a specified data file.
	 * 
	 * @param dataFile
	 *            a file
	 * @return a URL
	 * @throws MalformedURLException
	 */
	public String getDataFileUrl(File dataFile) throws MalformedURLException {
		String dataURL = getDataServerUrl();
		String path = dataFile.getAbsolutePath();
		int index = path.indexOf(Sorcer.getProperty(R_DATA_DIR));
		return dataURL + File.separator + path.substring(index);
	}

	public File getJunitDataDir(String junitDataDirName) {
		
		// locate the junit data relative to iGridHome and copy it to a scratch
		// location that can be served via webster
		File iGridHome = Sorcer.getHomeDir();
		logger.info("iGrid Home = " + iGridHome);

		// junitDataDirName must be relative to iGrid.Home
		
		logger.info("junitDataDirName " + junitDataDirName);
		return new File(iGridHome + junitDataDirName);

	}	
	
	/**
	 * Returns the requestor's scratch directory
	 * 
	 * @return a scratch directory
	 */
	 public File getUserHomeDir() {
		return new File(System.getProperty("user.home"));
	}
		
	 public Properties getProperties() {
		 return props;
	 }
	 
	/**
	 * Returns the URL of a scratch file at the requestor HTTP data server.
	 * 
	 * @param scratchFile
	 * @return the URL of a scratch file
	 * @throws MalformedURLException
	 */
	public URL getScratchURL(File scratchFile)
			throws MalformedURLException {
		return Sorcer.getScratchURL(scratchFile);
	}

	/**
	 * Returns the URL of a dataFile at the requestor HTTP data server.
	 * 
	 * @param dataFile
	 * @return the URL of a data file
	 * @throws MalformedURLException
	 */
	public static URL getDataURL(File dataFile)
			throws MalformedURLException {
		return Sorcer.getDataURL(dataFile);
	}
	
	protected static String[] toArray(String arg) {
		StringTokenizer token = new StringTokenizer(arg, " ,;");
		String[] array = new String[token.countTokens()];
		int i = 0;
		while (token.hasMoreTokens()) {
			array[i] = token.nextToken();
			i++;
		}
		return (array);
	}
		
}
