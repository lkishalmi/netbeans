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
package org.netbeans.modules.devops.project.spi;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import static org.netbeans.api.project.ProjectInformation.PROP_DISPLAY_NAME;
import static org.netbeans.api.project.ProjectInformation.PROP_ICON;
import static org.netbeans.api.project.ProjectInformation.PROP_NAME;
import org.netbeans.modules.devops.project.api.DevOpsProject;

/**
 *
 * @author lkishalmi
 */
public abstract class AbstractDevOpsProjectInfo implements ProjectInformation, PropertyChangeListener {

    protected final Project project;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public AbstractDevOpsProjectInfo(Project project) {
        this.project = project;
    }

    @Override
    public String getName() {
        return project.getProjectDirectory().getNameExt();
    }

    @Override
    public Icon getIcon() {
        return DevOpsProject.get(project).getIcon();
    }

    @Override
    public final Project getProject() {
        return project;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (!pcs.hasListeners(null)) {
            DevOpsProject.get(project).addPropertyChangeListener(this);
        }
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        boolean had = pcs.hasListeners(null);
        pcs.removePropertyChangeListener(listener);
        if (had && !pcs.hasListeners(null)) {
            DevOpsProject.get(project).removePropertyChangeListener(this);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (DevOpsProject.isProjectChange(evt)) {
            pcs.firePropertyChange(PROP_NAME, null, null);
            pcs.firePropertyChange(PROP_DISPLAY_NAME, null, null);
            pcs.firePropertyChange(PROP_ICON, null, null);
        }
    }
}
