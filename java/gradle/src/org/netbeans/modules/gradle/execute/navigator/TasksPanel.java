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

package org.netbeans.modules.gradle.execute.navigator;

import org.netbeans.modules.gradle.ActionProviderImpl;
import org.netbeans.modules.gradle.api.GradleProject;
import org.netbeans.modules.gradle.api.GradleTask;
import org.netbeans.modules.gradle.api.NbGradleProject;
import org.netbeans.modules.gradle.api.execute.ActionMapping;
import org.netbeans.modules.gradle.customizer.CustomActionMapping;
import org.netbeans.modules.gradle.spi.nodes.NodeUtils;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeSelectionModel;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

import static org.netbeans.modules.gradle.execute.navigator.Bundle.*;
import org.netbeans.modules.gradle.spi.Utils;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Laszlo Kishalmi
 */
public class TasksPanel extends javax.swing.JPanel implements ExplorerManager.Provider, Runnable {

    private final transient ExplorerManager manager = new ExplorerManager();
    private final BeanTreeView treeView;
    private NbGradleProject current;
    private Project currentP;

    private final PropertyChangeListener pchadapter = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (NbGradleProject.PROP_PROJECT_INFO.equals(evt.getPropertyName())) {
                showWaitNode();
                RequestProcessor.getDefault().post(TasksPanel.this);
            }
        }
    };

    /**
     * Creates new form TasksPanel
     */
    public TasksPanel() {
        initComponents();
        treeView = (BeanTreeView) taskPane;
        treeView.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        taskPane = new BeanTreeView();

        setLayout(new java.awt.BorderLayout());
        add(taskPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane taskPane;
    // End of variables declaration//GEN-END:variables

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }

    void navigate(DataObject d) {
        if (current != null) {
            current.removePropertyChangeListener(pchadapter);
        }
        NbGradleProject n = null;

        FileObject f = d.getPrimaryFile();
        if (!f.isFolder()) {
            f = f.getParent();
        }
        Project p = null;
        try {
            p = ProjectManager.getDefault().findProject(f);
            if (p != null) {
                n = NbGradleProject.get(p);
            }
        } catch (IOException | IllegalArgumentException ex) {
            //Ignore we can't really do anything about this.
        }

        if (n == null) {
            release();
            return;
        }

        current = n;
        currentP = p;
        current.addPropertyChangeListener(pchadapter);
        showWaitNode();
        RequestProcessor.getDefault().post(this);
    }

    @Override
    public void run() {
        if (currentP != null) {

            GradleProject prj = GradleProject.get(currentP);
            if (prj != null) {
                final Children ch = new Children.Array();
                ArrayList<String> glist = new ArrayList<>();
                for (String group : prj.getBaseProject().getTaskGroups()) {
                    if (!GradleProject.PRIVATE_TASK_GROUP.equals(group)) {
                        glist.add(group);
                    }
                }
                Collections.sort(glist, String.CASE_INSENSITIVE_ORDER);

                for (String group : glist) {
                    ch.add(new Node[]{new TaskGroupNode(group, prj)});
                }
                ch.add(new Node[]{new TaskGroupNode(GradleProject.PRIVATE_TASK_GROUP, prj)});

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        treeView.setRootVisible(false);
                        manager.setRootContext(new AbstractNode(ch));
                        treeView.expandAll();
                    }
                });
                return;
            }

        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                treeView.setRootVisible(false);
                manager.setRootContext(createEmptyNode());
            }
        });
    }

    void release() {
        if (current != null) {
            current.removePropertyChangeListener(pchadapter);
        }
        current = null;
        currentP = null;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                treeView.setRootVisible(false);
                manager.setRootContext(createEmptyNode());
            }
        });
    }

    /**
     *
     */
    public void showWaitNode() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                treeView.setRootVisible(true);
                manager.setRootContext(createWaitNode());
            }
        });
    }

    @Messages("LBL_Wait=Please Wait...")
    private static Node createWaitNode() {
        AbstractNode an = new AbstractNode(Children.LEAF);
        an.setIconBaseWithExtension(WAIT_GIF);
        an.setDisplayName(LBL_Wait());
        return an;
    }

    @StaticResource
    private static final String WAIT_GIF = "org/netbeans/modules/gradle/resources/wait.gif";

    @StaticResource
    private static final String TASK_ICON = "org/netbeans/modules/gradle/resources/gradle-task.png";

    private static Node createEmptyNode() {
        return new AbstractNode(Children.LEAF);
    }

    private class TaskGroupNode extends AbstractNode {

        @Messages({
            "LBL_PrivateTasks=Other Tasks"
        })
        @SuppressWarnings("OverridableMethodCallInConstructor")
        public TaskGroupNode(String group, GradleProject project) {
            super(Children.create(new TaskGroupChildren(group, project), true), Lookup.EMPTY);
            setName(group);
            String displayName = GradleProject.PRIVATE_TASK_GROUP.equals(group)
                    ? LBL_PrivateTasks()
                    : Utils.capitalize(group);
            setDisplayName(displayName);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(true);
        }

        @Override
        public Image getIcon(int type) {
            return getIcon(false);
        }

        private Image getIcon(boolean opened) {
            return NodeUtils.getTreeFolderIcon(opened);
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }

    }

    private class TaskNode extends AbstractNode {

        final GradleTask task;

        @SuppressWarnings("OverridableMethodCallInConstructor")
        public TaskNode(GradleTask task) {
            super(Children.LEAF);
            this.task = task;
            setIconBaseWithExtension(TASK_ICON);
            setName(task.getPath());
            setDisplayName(task.getName());
            setShortDescription(task.getDescription());
        }

        @Messages({
            "LBL_ExecTask=Run Task",
            "LBL_ExecCust=Execute Custom..."
        })
        @Override
        public Action[] getActions(boolean context) {
            CustomActionMapping mapping = new CustomActionMapping(ActionMapping.CUSTOM_PREFIX);
            mapping.setArgs(task.getName());

            return new Action[] {
                ActionProviderImpl.createCustomGradleAction(currentP, LBL_ExecTask(), mapping, Lookups.singleton(currentP), false),
                ActionProviderImpl.createCustomGradleAction(currentP, LBL_ExecCust(), mapping, Lookups.singleton(currentP), true),
            };
        }

        public GradleTask getTask() {
            return task;
        }

    }

    private class TaskGroupChildren extends ChildFactory<GradleTask> {

        private final String group;
        private final GradleProject project;

        public TaskGroupChildren(String group, GradleProject project) {
            this.group = group;
            this.project = project;
        }

        @Override
        protected boolean createKeys(List<GradleTask> list) {
            ArrayList<GradleTask> ret = new ArrayList<>(project.getBaseProject().getTasks(group));
            Collections.sort(ret, new Comparator<GradleTask>() {

                @Override
                public int compare(GradleTask o1, GradleTask o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            list.addAll(ret);
            return true;
        }

        @Override
        protected Node createNodeForKey(GradleTask key) {
            return new TaskNode(key);
        }

    }
}
