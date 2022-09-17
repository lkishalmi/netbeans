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
package org.netbeans.modules.helm.queries;

import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.helm.HelmProjectImpl;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.support.GenericSources;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author lkishalmi
 */
@ProjectServiceProvider(projectType = HelmProjectImpl.HELM_PROJECT_TYPE, service = Sources.class)
public class HelmSources implements Sources {

    public static final String TYPE_TEMPLATES = "templates";
    public static final String TYPE_TESTS = "test";
    
    private final Project project;

    public HelmSources(Project project) {
        this.project = project;
    }
    
    @Override
    public SourceGroup[] getSourceGroups(String type) {
        switch(type) {
            case Sources.TYPE_GENERIC:
                return new SourceGroup[] {
                    GenericSources.group(project, 
                        project.getProjectDirectory(), 
                        "deneric", 
                        ProjectUtils.getInformation(project).getDisplayName(),
                        null, null)
                };
            case TYPE_TEMPLATES:
                project.getProjectDirectory();
                return new SourceGroup[] {
                    GenericSources.group(project, 
                        project.getProjectDirectory(), 
                        "deneric", 
                        ProjectUtils.getInformation(project).getDisplayName(),
                        null, null)
                };
            default:
                return new SourceGroup[0];
        }
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
    }

}
