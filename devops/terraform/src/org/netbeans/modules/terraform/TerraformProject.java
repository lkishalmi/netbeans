/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.terraform;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.parsing.spi.indexing.PathRecognizerRegistration;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.CacheDirectoryProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author lkishalmi
 */
@PathRecognizerRegistration(
        sourcePathIds = TerraformProject.TERRAFORM_CLASSPATH,
        mimeTypes = TerraformLanguage.MIME_TYPE
)
public class TerraformProject implements Project {

    public static final String TERRAFORM_CLASSPATH = "classpath/terraform";
    private final FileObject projectDir;
    private final ProjectState state;
    private final Lookup lookup;

    public TerraformProject(FileObject projectDir, ProjectState state) {
        this.projectDir = projectDir;
        this.state = state;
        Lookup baseLookup = Lookups.fixed(
                this,
                state,
                projectDir,
                new CacheDirProvider(),
                new SourceGroups(),
                UILookupMergerSupport.createProjectOpenHookMerger(new OpenHook())
                );
        lookup = LookupProviderSupport.createCompositeLookup(baseLookup, "Projects/terraform/Lookup"); //NOI18N
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    @Override
    public Lookup getLookup() {
        return lookup;      
    }

    private static final String NB_CACHE_DIR = ".terraform/nb-cache";  //NOI18N

    private class CacheDirProvider implements CacheDirectoryProvider {

        @Override
        public FileObject getCacheDirectory() throws IOException {
            return FileUtil.createFolder(projectDir, NB_CACHE_DIR);
        }
    }

    private class OpenHook extends ProjectOpenedHook {
        private final ClassPath[] classpath = new ClassPath[] {ClassPathSupport.createClassPath(projectDir)};

        @Override
        protected void projectOpened() {
            GlobalPathRegistry.getDefault().register(TerraformLanguage.TERRAFORM_CLASSPATH, classpath );
        }

        @Override
        protected void projectClosed() {
            GlobalPathRegistry.getDefault().unregister(TerraformLanguage.TERRAFORM_CLASSPATH, classpath );
        }

    }

    private class SourceGroups implements Sources {

        private SourceGroup[] groups;

        @Override
        public SourceGroup[] getSourceGroups(String type) {
            if (groups == null) {
                groups = new SourceGroup[] {new TerraformSourceGroup()};
            }
            return Sources.TYPE_GENERIC.equals(type) ? groups : new SourceGroup[0];
        }

        @Override
        public void addChangeListener(ChangeListener listener) {
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
        }

    }

    private class TerraformSourceGroup implements SourceGroup {

        @Override
        public FileObject getRootFolder() {
            return projectDir;
        }

        @Override
        public String getName() {
            return ProjectUtils.getInformation(TerraformProject.this).getName();
        }

        @Override
        public String getDisplayName() {
            return ProjectUtils.getInformation(TerraformProject.this).getDisplayName();
        }

        @Override
        public Icon getIcon(boolean opened) {
            return null;
        }

        @Override
        public boolean contains(FileObject file) {
            return file.isData() && projectDir.equals(file.getParent());
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }

    }
}
