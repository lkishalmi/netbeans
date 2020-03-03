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
        Clusters clusters = Clusters.loadClusters(project.getRootDir());
        project.getExtensions().add("clusters", clusters);
        project.getSubprojects().forEach((Project subproject) -> {
            subproject.getPluginManager().apply(NetBeansModulePlugin.class);
        });
        clusters.clusters.forEach((String name, NbCluster cluster) -> {
            Task task = project.getTasks().create("build" + capitalize(name) + "Cluster");
            for (DependencyItem<? extends NbCluster> dependency : cluster.getDependencies()) {
                task.dependsOn("build" + capitalize(dependency.getName()) + "Cluster");
            }
            for (NbModule module : cluster.modules) {
                task.dependsOn(":" + module.getCodeNameBase() + ":build");
            }
        });
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
