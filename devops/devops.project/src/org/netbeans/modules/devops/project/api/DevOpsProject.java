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
package org.netbeans.modules.devops.project.api;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.devops.project.DevOpsProjectImpl;
import org.netbeans.modules.devops.project.spi.WatchedResourceProvider;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

/**
 *
 * @author lkishalmi
 */
public final class DevOpsProject {
    
    public static final String PROJECT_TYPE = "org-netbeans-modules-devops"; //NOI18N
    
    private static final String PROP_PROJECT = "project";   //NOI18N
    private static final String PROP_RESOURCE = "resource"; //NOI18N


    static {
        new Accessor().assign();
    }
    
    private static final class Accessor extends DevOpsProjectImpl.ProjectAccessor {

        public void assign() {
            if (DevOpsProjectImpl.ACCESSOR == null) {
                DevOpsProjectImpl.ACCESSOR = this;
            }
        }
        
        @Override
        public DevOpsProject createApiProject(DevOpsProjectImpl proj) {
            return new DevOpsProject(proj);
        }

        @Override
        public void doFireReload(DevOpsProject api) {
            api.doFireReload();
        }

        @Override
        public void activate(DevOpsProject api) {
            api.attachResourceWatchers();
        }

        @Override
        public void passivate(DevOpsProject api) {
            api.detachResourceWatchers();
        }
        
    }

    private final DevOpsProjectImpl impl;
    private final PropertyChangeSupport support;

    private final Set<File> resources = new HashSet<>();

    private Preferences privatePrefs;
    private Preferences sharedPrefs;

    
    private DevOpsProject(DevOpsProjectImpl project) {
        this.impl = project;
        this.support = new PropertyChangeSupport(project);
    }
    
    public String getProjectType() {
        return impl.getProjectType();
    }
    
    public ImageIcon getIcon() {
        return impl.getIcon();
    }
    
    public Image getIconImage() {
        return impl.getIconImage();
    }
    
    public Preferences getPreferences(boolean shared) {
        Preferences ret = shared ? sharedPrefs : privatePrefs;
        if (ret == null) {
            if (shared) {
                ret = sharedPrefs = ProjectUtils.getPreferences(impl, DevOpsProject.class, true);
            } else {
                ret = privatePrefs = ProjectUtils.getPreferences(impl, DevOpsProject.class, false);
            }
        }
        return ret;
    }

    private void fireProjectReload() {
        impl.fireProjectReload(false);
    }

    private void doFireReload() {
        detachResourceWatchers();
        support.firePropertyChange(PROP_PROJECT, null, null);
        attachResourceWatchers();
    }

    private void detachResourceWatchers() {
        synchronized (resources) {
            for (File resource : resources) {
                try {
                    FileUtil.removeFileChangeListener(FCHSL, resource);
                } catch (IllegalArgumentException ex) {
                    assert false : "Something is wrong with the resource handling";
                }
            }
            resources.clear();
        }
    }

    private void attachResourceWatchers() {
        synchronized (resources) {
            if (!resources.isEmpty()) {
                resources.clear();
            }
            Collection<? extends WatchedResourceProvider> all = impl.getLookup().lookupAll(WatchedResourceProvider.class);
            for (WatchedResourceProvider pvd : all) {
                resources.addAll(pvd.getWatchedResources());
            }
            for (File resource : resources) {
                try {
                    FileUtil.addFileChangeListener(FCHSL, resource);
                } catch (IllegalArgumentException ex) {
                    assert false : "Something is wrong with the resource handling";
                }
            }
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        support.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        support.removePropertyChangeListener(propertyChangeListener);
    }

    public static DevOpsProject get(Project project) {
        return project instanceof DevOpsProjectImpl ? ((DevOpsProjectImpl) project).getApiProject(): null;
    }
    
    @Override
    public String toString() {
        return "API for " + impl.toString(); //NOI18N
    }

    public static void addPropertyChangeListener(Project project, PropertyChangeListener l) {
        DevOpsProject api = DevOpsProject.get(project);
        if (api != null) {
            api.addPropertyChangeListener(l);
        } else {
            assert false : "Attempted to add PropertyChangeListener to project " + project; //NOI18N
        }
    }

    /**
     * Convenient method to remove a Property Listener from a Gradle project.
     *
     * @param project
     * @param l
     */
    public static void removePropertyChangeListener(Project project, PropertyChangeListener l) {
        DevOpsProject api = DevOpsProject.get(project);
        if (api != null) {
            api.removePropertyChangeListener(l);
        } else {
            assert false : "Attempted to remove PropertyChangeListener to project " + project; //NOI18N
        }
    }

    public static boolean isProjectChange(PropertyChangeEvent evt) {
        return PROP_PROJECT.equals(evt.getPropertyName());
    }
    
    public static boolean isResourceChange(PropertyChangeEvent evt) {
        return PROP_RESOURCE.equals(evt.getPropertyName());
    }
    
    public static void fireGradleProjectReload(Project prj) {
        if (prj != null) {
            DevOpsProject api = DevOpsProject.get(prj);
            if (api != null) {
                api.fireProjectReload();
            }
        }
    }

    public static Preferences getPreferences(Project project, boolean shared) {
        DevOpsProject watcher = DevOpsProject.get(project);
        return watcher.getPreferences(shared);
    }

    private void fireChange(File f) {
        support.firePropertyChange(PROP_RESOURCE, null, Utilities.toURI(f));
    }

    private final FileChangeListener FCHSL = new FileChangeListener() {

        @Override
        public void fileFolderCreated(FileEvent fe) {
            fireChange(FileUtil.toFile(fe.getFile()));
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            fireChange(FileUtil.toFile(fe.getFile()));
        }

        @Override
        public void fileChanged(FileEvent fe) {
            fireChange(FileUtil.toFile(fe.getFile()));
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            fireChange(FileUtil.toFile(fe.getFile()));
        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
            fireChange(FileUtil.toFile(fe.getFile()));
        }

        @Override
        public void fileAttributeChanged(FileAttributeEvent fe) {
        }

    };
    
}
