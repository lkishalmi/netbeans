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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import javax.swing.ImageIcon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.devops.project.DevOpsProjectImpl;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;

/**
 *
 * @author lkishalmi
 */
public abstract class DevOpsProjectFactory implements ProjectFactory2 {

    private final String projectType;
    private final String projectIconBase;

    public DevOpsProjectFactory(String projectType, String projectIconBase) {
        this.projectType = projectType;
        this.projectIconBase = projectIconBase;
    }
    
    @Override
    public final ProjectManager.Result isProject2(FileObject projectDirectory) {
        ProjectManager.Result ret = null;
        if (projectDirectory != null && isProject(projectDirectory)) {
            ret = new ProjectManager.Result(projectDirectory.getNameExt(), projectType, getIcon());
        }
        return ret;
    }

    @Override
    public final Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
        return new DevOpsProjectImpl(projectDirectory, state, projectType, projectIconBase, () -> getProjectFiles(projectDirectory));
    }

    @Override
    public final void saveProject(Project project) throws IOException, ClassCastException {
    }
    
    private ImageIcon getIcon() {
        return ImageUtilities.loadImageIcon(projectIconBase, false);
    }

    protected Set<File> getProjectFiles(FileObject projectDirectory) {
        return Collections.emptySet();
    }
}
