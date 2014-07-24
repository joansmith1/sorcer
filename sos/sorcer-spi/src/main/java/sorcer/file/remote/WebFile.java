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

package sorcer.file.remote;

import com.google.common.io.Closer;
import com.google.common.io.Resources;
import sorcer.core.SorcerEnv;
import sorcer.util.IOUtils;

import java.io.*;
import java.net.URL;

/**
 * @author Rafał Krupiński
 */
public class WebFile extends AbstractRemoteFile implements Serializable {
    private final File dataDir;
    private URL remoteUrl;

    public WebFile(File dataDir, File localFile) throws IOException {
        super(localFile);
        this.dataDir = dataDir;
        setLocalFile(localFile);
    }

    @Override
    protected File doGetFile() throws IOException {
        File localFile = getLocalPath();
        Closer closer = Closer.create();
        try {
            FileOutputStream local = closer.register(new FileOutputStream(localFile));
            Resources.copy(remoteUrl, local);
        } finally {
            closer.close();
        }
        return localFile;
    }

    @Override
    protected File getLocalPath() {
        return new File(SorcerEnv.getScratchDir(), checksum);
    }

    protected void setLocalFile(File localFile) throws IOException {
        if (!IOUtils.isChild(dataDir, localFile)) {
            File my = new File(dataDir, localFile.getName());
            IOUtils.copyLarge(new FileInputStream(localFile), new FileOutputStream(my));
            remoteUrl = SorcerEnv.getDataURL(my);
        }
    }
}