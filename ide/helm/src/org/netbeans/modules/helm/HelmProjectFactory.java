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
package org.netbeans.modules.helm;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lkishalmi
 */
@ServiceProvider(service = ProjectFactory.class, position = 42)
public final class HelmProjectFactory implements ProjectFactory2 {

    @Override
    public ProjectManager.Result isProject2(FileObject projectDirectory) {
        return isProject(projectDirectory)
                ? new ProjectManager.Result(projectDirectory.getName(), HelmProjectImpl.HELM_PROJECT_TYPE, HelmProjectImpl.getIcon())
                : null;
    }

    @Override
    public boolean isProject(FileObject fo) {
        if (fo == null) return false;

        FileObject chart = fo.getFileObject("Chart.yaml");
        FileObject templates = fo.getFileObject("templates");
        return (chart != null) && (templates != null) && chart.isData() && templates.isFolder();
    }

    @Override
    public Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
        return new HelmProjectImpl(projectDirectory, state);
    }

    @Override
    public void saveProject(Project project) throws IOException, ClassCastException {
    }

}
