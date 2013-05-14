/**
 *
 * Copyright 2013 the original author or authors.
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
package sorcer.util;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;

/**
 * @author Rafał Krupiński
 */
public class PolicyFileHelper {
	public static String preparePolicyFile(String outputDirectory) throws MojoFailureException {
		File policy = new File(outputDirectory, "sorcer.policy");
		if (!policy.exists()) {
			try {
				FileUtils.write(policy, "grant {permission java.security.AllPermission;};");
			} catch (IOException e) {
				throw new MojoFailureException("could not write to " + outputDirectory, e);
			}
		}
		return policy.getPath();
	}
}