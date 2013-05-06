package sorcer.util;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoFailureException;
import sorcer.maven.plugin.BootMojo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.Properties;
import java.util.Random;

/**
 * @author Rafał Krupiński
 */
public class EnvFileHelper {
	public static String prepareEnvFile(String projectOutDir) throws MojoFailureException {
		Properties props = new Properties();
		URL resourceUrl = BootMojo.class.getClassLoader().getResource("META-INF/sorcer-maven-plugin/sorcer.env");
		if (resourceUrl == null) {
			throw new MojoFailureException("Could not find internal sorcer.env");
		}
		InputStream inputStream = null;
		FileOutputStream outProps = null;
		try {
			inputStream = resourceUrl.openStream();
			props.load(inputStream);
			String id = getRandomString();
			props.setProperty("provider.groups", id);

			File propFile = new File(projectOutDir, "sorcer.env");
			outProps = new FileOutputStream(propFile);
			props.store(outProps, "Generated by sorcer-maven-plugin");

			return propFile.getPath();
		} catch (IOException e) {
			throw new MojoFailureException("Error while reading internal sorcer.env", e);
		} finally {
			IOUtils.closeQuietly(inputStream);
			IOUtils.closeQuietly(outProps);
		}
	}

	/**
	 * prepare random 8 alphanumeric character id
	 *
	 * @return
	 */
	protected static String getRandomString() {
		return new BigInteger(64, new Random()).toString(16);
	}
}
