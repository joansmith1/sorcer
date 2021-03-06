package sorcer.resolver;
/*
 * Copyright 2014 Sorcersoft.com S.A.
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


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sorcer.core.SorcerEnv;
import sorcer.util.ArtifactCoordinates;
import sorcer.util.JavaSystemProperties;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Rafał Krupiński
 */
public class ProjectArtifactResolver implements ArtifactResolver {
    private static final Logger log = LoggerFactory.getLogger(ProjectArtifactResolver.class);
    private static final IOFileFilter DIR_FILTER = FileFilterUtils.and(DirectoryFileFilter.INSTANCE, HiddenFileFilter.VISIBLE);

    private Collection<File> roots;

    public ProjectArtifactResolver() throws IOException {
        File homeDir = SorcerEnv.getHomeDir().getCanonicalFile();
        File userDir = new File(System.getProperty(JavaSystemProperties.USER_DIR)).getCanonicalFile();
        File extDir;
        try {
            extDir = SorcerEnv.getExtDir().getCanonicalFile();
        } catch (IOException e) {
            log.debug("Error canonizing path {}", SorcerEnv.getExtDir(), e);
            extDir = null;
        }

        roots = buildRoots(homeDir, userDir, extDir);

        if (log.isDebugEnabled())
            for (File root : roots)
                log.debug("Artifact search root: {}", root);
    }

    private Collection<File> buildRoots(File... roots) {
        Set<File> result = new HashSet<File>();
        FILE:
        for (File file : roots) {
            if (file == null || !file.exists() || !file.isDirectory())
                continue;
            for (File other : result) {
                //replace child with its parent
                if (isParent(file, other)) {
                    result.remove(other);
                    result.add(file);
                    continue FILE;
                } else if (isParent(other, file))
                    // don't add file if its parent is in the result.
                    continue FILE;
            }
            result.add(file);
        }
        return result;
    }

    private boolean isParent(File parent, File child) {
        File f = child;
        while ((f = f.getParentFile()) != null) {
            if (f.equals(parent))
                return true;

        }
        return false;
    }

    @Override
    public String resolveAbsolute(ArtifactCoordinates artifactCoordinates) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String resolveAbsolute(String artifactCoordinates) {
        for (File root : roots) {
            File result = resolveRelative(root, artifactCoordinates);
            if (result != null)
                return result.getPath();
        }
        return null;
    }

    @Override
    public String resolveRelative(ArtifactCoordinates artifactCoordinates) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String resolveRelative(String artifactCoordinates) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRootDir() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String resolveSimpleName(String simpleName, String packaging) {
        return null;
    }

    protected File resolveRelative(File root, String artifactId) {
        Collection<File> files = FileUtils.listFiles(root, new ArtifactIdFileFilter(artifactId), DIR_FILTER);
        if (files.size() > 0) {
            File result = files.iterator().next();
            if (files.size() > 1) {
                // Trying to eliminate the ones that have sth additional in its name,
                // i.e. looking for soap-client but found soap-client-req
                List<File> fileList = new ArrayList<File>(files);
                Collections.sort(fileList, new FileNameLengthComparator());
                result = fileList.get(0);
                log.warn("Found {} files in {} possibly matching artifactId, using the one with the shortest name: {}", files.size(), root, result);
                log.debug("Files found: {}", files);
            }
            return result;
        }
        return null;
    }
}

class FileNameLengthComparator implements Comparator<File>{
    @Override
    public int compare(File f1, File f2) {
        if ((f1.getName()==null) && (f2.getName()==null)) return 0;
        if (f1.getName()==null) return -1;
        if (f2.getName()==null) return 1;

        if (f1.getName().length()>f2.getName().length())
            return 1;
        else if (f1.getName().length()<f2.getName().length())
            return -1;
        else
            return f1.getName().compareTo(f2.getName());
    }
}

class ArtifactIdFileFilter extends AbstractFileFilter {
    private String artifactId;
    private String type;

    public ArtifactIdFileFilter(String input) {
        String[] inputPart = input.split(":");
        this.artifactId = inputPart[0];
        if (inputPart.length>1)
            this.type = "." + inputPart[1];
        else
            this.type = ".jar";
    }

    @Override
    public boolean accept(File dir, String name) {
        String parent = dir.getName();
        String grandParent = dir.getParentFile().getName();
        return
                new File(dir, name).isFile() && name.startsWith(artifactId + "-") && name.endsWith(type) && (
                        //check development structure
                        "target".equals(parent)
                                //check repository just in case
                                || artifactId.equals(grandParent)

                        || (
                            //check gradle development structure
                            "libs".equals(parent)
                            || "config".equals(parent)
                                    //check repository just in case
                            || "build".equals(grandParent)
                        )
                )
                //check distribution structure
                || "lib".equals(grandParent) && (artifactId + type).equals(name)
                ;
    }
}
