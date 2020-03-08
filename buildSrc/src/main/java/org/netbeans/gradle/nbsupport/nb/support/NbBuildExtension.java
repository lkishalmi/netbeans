/*
 * Copyright 2020 lkishalmi.
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
package org.netbeans.gradle.nbsupport.nb.support;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import org.gradle.tooling.BuildException;

/**
 *
 * @author lkishalmi
 */
public final class NbBuildExtension {

    Set<String> annotationProcessors = new HashSet<>(Arrays.asList(
        "org.openide.util.lookup",
        "org.openide.util",
        "org.openide.modules",
        "org.netbeans.modules.editor.mimelookup",
        "org.openide.filesystems",

        "net.java.html.boot",
        "net.java.html.sound",
        "net.java.html.geo",
        "net.java.html.json"

    ));

    boolean generateCopyExternals = true;

    public NbBuildExtension() {
    }

    public Set<String> getAnnotationProcessors() {
        return annotationProcessors;
    }

    public void setAnnotationProcessors(Set<String> annotationProcessors) {
        this.annotationProcessors = annotationProcessors;
    }

    public void annotationProcessor(String proc) {
        annotationProcessors.add(proc);
    }

    public boolean isGenerateCopyExternals() {
        return generateCopyExternals;
    }

    public void setGenerateCopyExternals(boolean generateCopyExternals) {
        this.generateCopyExternals = generateCopyExternals;
    }

    public String fileCRC32(File file) {
        CRC32 crc32 = new CRC32();
        try(FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            MappedByteBuffer mbb = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            crc32.update(mbb);
            return Long.toString(crc32.getValue());
        } catch (IOException ex) {
            throw new BuildException("Can't caclulate CRC32 as of", ex);
        }
    }
}
