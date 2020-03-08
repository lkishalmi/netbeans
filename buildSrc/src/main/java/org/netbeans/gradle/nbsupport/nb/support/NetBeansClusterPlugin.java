/*
 * Copyright 2020 lkishalmi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.netbeans.gradle.nbsupport.nb.support;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

/**
 *
 * @author lkishalmi
 */
public class NetBeansClusterPlugin implements Plugin<Project>{

    @Override
    public void apply(Project project) {
        for (Project subproject : project.getSubprojects()) {
            NbBuildExtension nbbuild = new NbBuildExtension();
            NbProjectExtension nbproject = new NbProjectExtension(subproject);
            subproject.getExtensions().add("nbbuild", nbbuild);
            subproject.getExtensions().add("nbproject", nbproject);
        }
        for (Project subproject : project.getSubprojects()) {
            subproject.getPluginManager().apply(NetBeansModulePlugin.class);
        }
        NbClusterContainer clusters = project.getObjects().newInstance(DefaultNbClusterContainer.class, project);
        project.getExtensions().add("clusters", clusters);
        project.afterEvaluate((Project prj) -> {
            NbClusterContainer c = prj.getExtensions().getByType(NbClusterContainer.class);
            for (NbCluster cluster : c) {
                prj.getTasks().register(getClusterBuildTask(cluster.getName()), (Task task) -> {
                    task.setGroup("build");
                    task.setDescription("Assembles and test the NetBeans " + cluster.getName() + " cluster");
                    for (DependencyItem<? extends NbCluster> dep : cluster.getDependencies()) {
                        task.dependsOn(getClusterBuildTask(dep.getName()));
                    }
                    for (String module : cluster.modules) {
                        task.dependsOn(":" + module + ":build");
                    }
                });
            }
        });
    }

    private static String getClusterBuildTask(String clusterName) {
        return "build" + capitalize(clusterName) + "Cluster";
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
