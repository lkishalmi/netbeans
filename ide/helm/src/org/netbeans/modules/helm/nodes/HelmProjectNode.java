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
package org.netbeans.modules.helm.nodes;

import java.awt.Image;
import javax.swing.Action;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.helm.HelmChart;
import org.netbeans.modules.helm.HelmProjectImpl;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.openide.nodes.AbstractNode;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

/**
 *
 * @author lkishalmi
 */
public class HelmProjectNode extends AbstractNode {
    
    private final ProjectInformation info;    
    private final HelmProjectImpl project;
    
    public HelmProjectNode(HelmProjectImpl project, Lookup lookup) {
        super(NodeFactorySupport.createCompositeChildren(project, "Projects/" + HelmProjectImpl.HELM_PROJECT_TYPE + "/Nodes"), lookup);
        this.project = project;
        this.info = ProjectUtils.getInformation(project);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return ImageUtilities.icon2Image(HelmProjectImpl.getIcon());
    }

    @Override
    public Image getIcon(int type) {
        return ImageUtilities.icon2Image(HelmProjectImpl.getIcon());
    }

    @Override
    public String getShortDescription() {
        HelmChart chart = project.getHelmChart();
        return (chart != null) && chart.getDescription().isPresent() ? chart.getDescription().get() : null;
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
        return CommonProjectActions.forType(HelmProjectImpl.HELM_PROJECT_TYPE);
    }
    
}
