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

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.modules.devops.project.spi.AbstractDevOpsProjectInfo;
import org.netbeans.spi.project.ProjectServiceProvider;

/**
 *
 * @author lkishalmi
 */
@ProjectServiceProvider(service = ProjectInformation.class, projectType = HelmProjectFactory.PROJECT_TYPE)
public class ProjectInfo extends AbstractDevOpsProjectInfo {

    public ProjectInfo(Project project) {
        super(project);
    }
   
    @Override
    public String getName() {
        HelmChart chart = getHelmChart();
        return chart != null ? chart.name : "Broken Chart: " + project.getProjectDirectory().getName();
    }

    @Override
    public String getDisplayName() {
        HelmChart chart = getHelmChart();
        return chart != null ? chart.name + ":" + chart.version : "Broken Chart: " + project.getProjectDirectory().getName();        
    }
    
    private HelmChart getHelmChart() {
        return project.getLookup().lookup(HelmChart.class);
    }
}
