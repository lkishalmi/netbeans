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
package org.netbeans.modules.devops.project;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.ImageIcon;
import org.netbeans.api.project.Project;
import org.netbeans.modules.devops.project.api.DevOpsProject;
import org.netbeans.modules.devops.project.spi.ProjectDataLoader;
import org.netbeans.spi.project.CacheDirectoryProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author lkishalmi
 */
public class DevOpsProjectImpl implements Project {

    public static final RequestProcessor RELOAD_RP = new RequestProcessor("DevOps project reloading"); //NOI18
    private final RequestProcessor.Task reloadTask = RELOAD_RP.create(this::reloadProjectData);

    public static ProjectAccessor ACCESSOR = null;    
    static {
        // that will assign value to the ACCESSOR field above
        try {
            Class.forName(DevOpsProject.class.getName(), true, DevOpsProjectImpl.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
        }
    }
    
    public abstract static class ProjectAccessor {

        public abstract DevOpsProject createApiProject(DevOpsProjectImpl proj);

        public abstract void doFireReload(DevOpsProject watcher);

        public abstract void activate(DevOpsProject watcher);

        public abstract void passivate(DevOpsProject watcher);
    }

    private final FileObject projectDir;
    private final String projectType;
    private final String projectIconBase;
    private final Lookup lookup;
    private final Supplier<Set<File>> fileSupplier;
    private final DevOpsProject apiProject;
    private final InstanceContent projectData = new InstanceContent();

    private Updater openedProjectUpdater;
    
    @SuppressWarnings("LeakingThisInConstructor") 
    public DevOpsProjectImpl(FileObject projectDir, ProjectState state, String projectType, String projectIconBase, Supplier<Set<File>> fileSupplier) {
        this.projectDir = projectDir;
        this.projectType = projectType;
        this.projectIconBase = projectIconBase;
        this.fileSupplier = fileSupplier != null ? fileSupplier : Collections::emptySet;
        this.apiProject = ACCESSOR.createApiProject(this);
        this.lookup = new ProxyLookup(
                new AbstractLookup(projectData),
                LookupProviderSupport.createCompositeLookup(
                    createLookup(state),
                    new ProxyLookup(
                        Lookups.forPath("Projects/" + projectType + "/Lookup"), //NOI18N
                        Lookups.forPath("Projects/" + DevOpsProject.PROJECT_TYPE + "/Lookup") //NOI18N
                    )
                ));
    }
    

    private Lookup createLookup(ProjectState state) {
        return Lookups.fixed(
                this,
                apiProject,
                projectDir,
                new CacheDirProvider(),
                UILookupMergerSupport.createProjectOpenHookMerger(new ProjectOpenHookImpl()),
                UILookupMergerSupport.createProjectProblemsProviderMerger(),
                UILookupMergerSupport.createRecommendedTemplatesMerger(),
                UILookupMergerSupport.createPrivilegedTemplatesMerger(),
                LookupProviderSupport.createSourcesMerger(),
                state
        );
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public String getProjectType() {
        return projectType;
    }
    
    public DevOpsProject getApiProject() {
        return apiProject;
    }
    
    public ImageIcon getIcon() {
        return ImageUtilities.loadImageIcon(projectIconBase, false);
    }
    
    public Image getIconImage() {
        return ImageUtilities.loadImage(projectIconBase, false);
    }
    
    private class ProjectOpenHookImpl extends ProjectOpenedHook {

        @Override
        protected void projectOpened() {
            fireProjectReload(true);
            attachAllUpdater();
        }

        @Override
        protected void projectClosed() {
            detachAllUpdater();
            projectData.set(Collections.emptySet(), null);
        }
        
    }

    private class CacheDirProvider implements CacheDirectoryProvider {

        @Override
        public FileObject getCacheDirectory() throws IOException {
            return FileUtil.createFolder(projectDir, ".nbproject");
        }
    }
    
    public void fireProjectReload(boolean wait) {
        reloadTask.schedule(0);
        if (wait) {
            reloadTask.waitFinished();
        }
    }

    private void reloadProjectData() {
        Collection<? extends ProjectDataLoader> loaders = lookup.lookupAll(ProjectDataLoader.class);
        Set<Object> data = new HashSet<>();
        for (ProjectDataLoader loader : loaders) {
            data.addAll(loader.loadProjectData());
        }
        projectData.set(data, null);
        ACCESSOR.doFireReload(apiProject);
    }
    
    void attachAllUpdater() {
        synchronized (this) {
            if (openedProjectUpdater == null) {
                openedProjectUpdater = new Updater();
            }
        }

        openedProjectUpdater.attachAll();
    }

    void detachAllUpdater() {
        synchronized (this) {
            if (openedProjectUpdater != null) {
                openedProjectUpdater.detachAll();
                openedProjectUpdater = null;
            }
        }
    }

    private class Updater implements FileChangeListener {

        Set<File> filesToWatch;
        long lastEventTime = 0;

        @Override
        public void fileFolderCreated(FileEvent fe) {
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            if (lastEventTime < fe.getTime()) {
                lastEventTime = System.currentTimeMillis();
                fireProjectReload(false);
            }
        }

        @Override
        public void fileChanged(FileEvent fe) {
            if (lastEventTime < fe.getTime()) {
                lastEventTime = System.currentTimeMillis();
                fireProjectReload(false);
            }
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            lastEventTime = System.currentTimeMillis();
            fireProjectReload(false);
        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
        }

        @Override
        public void fileAttributeChanged(FileAttributeEvent fe) {
        }

        synchronized void attachAll() {
            filesToWatch = fileSupplier.get();
            if (filesToWatch != null) {
                for (File f : filesToWatch) {
                    if (f != null) {
                        try {
                            FileUtil.addFileChangeListener(this, f);
                        } catch (IllegalArgumentException ex) {
                            assert false : "Project opened twice in a row";
                        }
                    }
                }
            }
        }

        synchronized void detachAll() {
            if (filesToWatch != null) {
                for (File f : filesToWatch) {
                    if (f != null) {
                        try {
                            FileUtil.removeFileChangeListener(this, f);
                        } catch (IllegalArgumentException ex) {
                            assert false : "Project closed twice in a row";
                        }
                    }
                }
            }
        }
    }
}
