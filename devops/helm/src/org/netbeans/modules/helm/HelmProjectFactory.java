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

import java.io.File;
import java.util.Set;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.modules.devops.project.spi.DevOpsProjectFactory;
import org.netbeans.spi.project.ProjectFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;
import static org.netbeans.modules.helm.HelmProjectFactory.PROJECT_TYPE;

@ActionReferences({/*
                <file name="org-netbeans-modules-project-ui-NewFile$WithSubMenu.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-NewFile$WithSubMenu.instance"/>
                    <attr name="position" intvalue="100"/>
                </file>
                <file name="sep-1.instance">
                    <attr name="instanceClass" stringvalue="javax.swing.JSeparator"/>
                    <attr name="position" intvalue="200"/>
                </file>
                <file name="org-netbeans-modules-project-ui-BuildProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-BuildProject.instance"/>
                    <attr name="position" intvalue="300"/>
                </file>
                <file name="org-netbeans-modules-project-ui-RebuildProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-RebuildProject.instance"/>
                    <attr name="position" intvalue="400"/>
                </file>
                <file name="org-netbeans-modules-project-ui-CleanProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-CleanProject.instance"/>
                    <attr name="position" intvalue="600"/>
                </file>
                <file name="org-netbeans-modules-project-ui-JavadocProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-JavadocProject.instance"/>
                    <attr name="position" intvalue="700"/>
                </file>
                <file name="sep-2.instance">
                    <attr name="instanceClass" stringvalue="javax.swing.JSeparator"/>
                    <attr name="position" intvalue="800"/>
                </file>
                <file name="org-netbeans-modules-project-ui-RunProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-RunProject.instance"/>
                    <attr name="position" intvalue="900"/>
                </file>
                <file name="org-netbeans-modules-debugger-ui-actions-DebugProjectAction.shadow">
                    <attr name="originalFile" stringvalue="Actions/Debug/org-netbeans-modules-debugger-ui-actions-DebugProjectAction.instance"/>
                    <attr name="position" intvalue="1000"/>
                </file>
                <file name="org-netbeans-modules-profiler-actions-ProfileProjectActionPopup.shadow">
                    <attr name="originalFile" stringvalue="Actions/Profile/org-netbeans-modules-profiler-actions-ProfileProjectPopup.instance"/>
                    <attr name="position" intvalue="1100"/>
                </file>
                <file name="org-netbeans-modules-project-ui-TestProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-TestProject.instance"/>
                    <attr name="position" intvalue="1200"/>
                </file>
                <file name="sep-3.instance">
                    <attr name="instanceClass" stringvalue="javax.swing.JSeparator"/>
                    <attr name="position" intvalue="1300"/>
                </file>
                <file name="org-netbeans-modules-project-ui-problems-BrokenProjectActionFactory.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-problems-BrokenProjectActionFactory.instance"/>
                    <attr name="position" intvalue="1770"/>
                </file>
                <file name="org-netbeans-modules-project-ui-SetMainProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-SetMainProject.instance"/>
                    <attr name="position" intvalue="1800"/>
                </file>
                <file name="org-netbeans-modules-project-ui-actions-OpenSubprojects.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-actions-OpenSubprojects.instance"/>
                    <attr name="position" intvalue="1900"/>
                </file>
                <file name="org-netbeans-modules-project-ui-CloseProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-CloseProject.instance"/>
                    <attr name="position" intvalue="2100"/>
                </file>
                <file name="sep-5.instance">
                    <attr name="instanceClass" stringvalue="javax.swing.JSeparator"/>
                    <attr name="position" intvalue="2200"/>
                </file>
                <file name="org-netbeans-modules-project-ui-DeleteProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-DeleteProject.instance"/>
                    <attr name="position" intvalue="2600"/>
                </file>
                <file name="sep-6.instance">
                    <attr name="instanceClass" stringvalue="javax.swing.JSeparator"/>
                    <attr name="position" intvalue="2700"/>
                </file>
                <file name="org-openide-actions-FindAction.shadow">
                    <attr name="originalFile" stringvalue="Actions/Edit/org-openide-actions-FindAction.instance"/>
                    <attr name="position" intvalue="2800"/>
                </file>
                <file name="general.shadow">
                    <attr name="originalFile" stringvalue="Projects/Actions"/>
                    <attr name="position" intvalue="2900"/>
                </file>
                <file name="sep-7.instance">
                    <attr name="instanceClass" stringvalue="javax.swing.JSeparator"/>
                    <attr name="position" intvalue="3000"/>
                </file>
                <file name="org-netbeans-modules-project-ui-CustomizeProject.shadow">
                    <attr name="originalFile" stringvalue="Actions/Project/org-netbeans-modules-project-ui-CustomizeProject.instance"/>
                    <attr name="position" intvalue="3200"/>
                </file>*/
    @ActionReference(
            path = HelmProjectFactory.PROJECT_ACTION_PATH,
            
            id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.SetMainProject"),
            position = 1800
    ),
    @ActionReference(
            path = HelmProjectFactory.PROJECT_ACTION_PATH,
            
            id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.actions.OpenSubprojects"),
            position = 1900
    ),
    @ActionReference(
            path = HelmProjectFactory.PROJECT_ACTION_PATH,
            
            id = @ActionID(category = "Project", id = "org.netbeans.modules.project.ui.CloseProject"),
            position = 2100
    )
})
@ServiceProvider(service = ProjectFactory.class, position = 42)
public final class HelmProjectFactory extends DevOpsProjectFactory {

    public static final String PROJECT_TYPE = "org-netbeans-modules-helm"; //NOI18N
    static final String PROJECT_ACTION_PATH = "Projects/" + PROJECT_TYPE + "/Actions"; //NOI18N

    @StaticResource
    private static final String HELM_ICON = "org/netbeans/modules/helm/helm-icon.png"; //NOI18
    
    public HelmProjectFactory() {
        super(PROJECT_TYPE, HELM_ICON);
    }

    @Override
    public boolean isProject(FileObject fo) {
        if (fo == null) return false;

        FileObject chart = fo.getFileObject("Chart.yaml");
        FileObject templates = fo.getFileObject("templates");
        return (chart != null) && (templates != null) && chart.isData() && templates.isFolder();
    }

    @Override
    protected Set<File> getProjectFiles(FileObject projectDirectory) {
        return super.getProjectFiles(projectDirectory); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    
}
