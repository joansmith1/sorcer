package sorcer.util;

import java.io.File;

import sorcer.core.SorcerConstants;

/**
 * @author Rafał Krupiński
 */
public class ArtifactCoordinates {
	private static final String DEFAULT_PACKAGING = "jar";
	private String groupId;
	private String artifactId;
	private String version;
	private String classifier;
	private String packaging;

	/**
	 * 
	 * @param coords
	 *            artifact coordinates in the form of
	 *            groupId:artifactId[[:packaging[:classifier]]:version]
	 * @throws IllegalArgumentException
	 */
	public static ArtifactCoordinates coords(String coords) {
		String[] coordSplit = coords.split(":");
		int length = coordSplit.length;
		if (length < 2 || length > 5) {
			throw new IllegalArgumentException(
					"Artifact coordinates must be in a form of groupId:artifactId[[:packaging[:classifier]]:version]");
		}
		String groupId = coordSplit[0];
		String artifactId = coordSplit[1];
		String packaging = DEFAULT_PACKAGING;
		String classifier = null;
		String version = SorcerConstants.SORCER_VERSION;

		if (length == 3) {
			version = coordSplit[2];
		} else if (length == 4) {
			packaging = coordSplit[2];
			version = coordSplit[3];
		} else if (length == 5) {
			packaging = coordSplit[2];
			classifier = coordSplit[3];
			version = coordSplit[4];
		}

		return new ArtifactCoordinates(groupId, artifactId, packaging, version, classifier);
	}

	public ArtifactCoordinates(String groupId, String artifactId, String packaging, String version, String classifier) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.packaging = packaging;
		this.version = version;
		this.classifier = classifier;
	}

	public static ArtifactCoordinates coords(String groupId, String artifactId, String version) {
		return new ArtifactCoordinates(groupId, artifactId, DEFAULT_PACKAGING, version, null);
	}

	public static ArtifactCoordinates coords(String groupId, String artifactId) {
		return new ArtifactCoordinates(groupId, artifactId, DEFAULT_PACKAGING, SorcerConstants.SORCER_VERSION, null);
	}

	public ArtifactCoordinates(String groupId, String artifactId, String version) {
		this(groupId, artifactId, DEFAULT_PACKAGING, version, null);
	}

	public ArtifactCoordinates(String groupId, String artifactId) {
		this(groupId, artifactId, DEFAULT_PACKAGING, SorcerConstants.SORCER_VERSION, null);
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public String getRelativePath() {
		StringBuilder result = new StringBuilder(groupId.replace('.', '/'));
		result.append('/').append(artifactId).append('/').append(version).append('/').append(artifactId).append('-')
				.append(version);
		if (classifier != null) {
			result.append('-').append(classifier);
		}
		result.append('.').append(packaging);
		return result.toString();
	}

	@Override
	public String toString() {
		return getRelativePath();
	}

	public File fileFromLocalRepo() {
		File repo = new File(System.getProperty("user.home"), ".m2/repository");
		return new File(repo, getRelativePath());
	}

	public String fromLocalRepo() {
		return fileFromLocalRepo().getPath();
	}

	public static String getSorcerApi() {
		return new ArtifactCoordinates("org.sorcersoft.sorcer", "sorcer-api").toString();
	}

	public static String getSorcerConst() {
		return new ArtifactCoordinates("org.sorcersoft.sorcer", "sorcer-const").toString();
	}

	public static String getDbpService() {
		return new ArtifactCoordinates("org.sorcersoft.sorcer", "dbp-service").toString();
	}

	public static String getJobberService() {
		return new ArtifactCoordinates("org.sorcersoft.sorcer", "jobber-service").toString();
	}
}
