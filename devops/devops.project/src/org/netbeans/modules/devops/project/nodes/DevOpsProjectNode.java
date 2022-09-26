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
package org.netbeans.modules.devops.project.nodes;

import java.awt.Image;
import javax.swing.Action;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.devops.project.DevOpsProjectImpl;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.openide.nodes.AbstractNode;
import org.openide.util.Lookup;

/**
 *
 * @author lkishalmi
 */
public class DevOpsProjectNode extends AbstractNode {
    
    private final ProjectInformation info;    
    private final DevOpsProjectImpl project;
    
    public DevOpsProjectNode(DevOpsProjectImpl project, Lookup lookup) {
        super(NodeFactorySupport.createCompositeChildren(project, "Projects/" + project.getProjectType() + "/Nodes"), lookup);
        this.project = project;
        this.info = ProjectUtils.getInformation(project);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return project.getIconImage();
    }

    @Override
    public Image getIcon(int type) {
        return project.getIconImage();
    }

    @Override
    public String getShortDescription() {
        return getName();
    }

    @Override
    public String getDisplayName() {
        return info.getDisplayName();
    }

    @Override
    public String getName() {
        return info.getName();
    }
    
    @Override
    public Action[] getActions(boolean param) {
        return CommonProjectActions.forType(project.getProjectType());
    }
    
}
