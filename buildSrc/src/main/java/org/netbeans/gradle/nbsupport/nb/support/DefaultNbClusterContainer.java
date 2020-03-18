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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import org.gradle.api.internal.AbstractValidatingNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.reflect.Instantiator;

/**
 *
 * @author lkishalmi
 */
public class DefaultNbClusterContainer extends AbstractValidatingNamedDomainObjectContainer<NbCluster> implements NbClusterContainer {

    private static final String CONFIG_PREFIX = "clusters.config.";
    private static final String CLUSTER_PREFIX = "nb.cluster.";
    private static final String DIR_SUFFIX = ".dir";
    private static final String DEPENDS_SUFFIX = ".depends";
    private static final String LIST_SUFFIX = ".list";

    private final Project project;

    @Inject
    public DefaultNbClusterContainer(Project project, Instantiator instantiator, CollectionCallbackActionDecorator callbackActionDecorator) {
        super(NbCluster.class, instantiator, (NbCluster c) -> c.getName(), callbackActionDecorator);
        this.project = project;
    }

    @Override
    protected NbCluster doCreate(String name) {
        NbCluster cluster = new NbCluster(name, new File(project.getRootDir(), name));
        return cluster;
    }

    @Override
    public TypeOf<?> getPublicType() {
        return TypeOf.typeOf(NbClusterContainer.class);
    }


    @Override
    public void from(File nbclusters) {
        Map<File, String> projectsByDir = new HashMap<>();
        for (Project subproject : project.getSubprojects()) {
            projectsByDir.put(subproject.getProjectDir(), subproject.getName());
        }

        Properties props = new ExpandingProperties();
        try ( Reader rd = new FileReader(nbclusters)) {
            props.load(rd);
        } catch (IOException ex) {
        }
        for (String propName : props.stringPropertyNames()) {
            if (propName.startsWith(CLUSTER_PREFIX) && propName.endsWith(DIR_SUFFIX)) {
                String clusterName = propName.substring(CLUSTER_PREFIX.length(), propName.length() - DIR_SUFFIX.length());
                String moduleList = props.getProperty(CLUSTER_PREFIX + clusterName);
                String[] modules = moduleList.split(",");
                File clusterDir = new File(project.getRootDir(), clusterName);
                Set<String> codeBaseNames = new LinkedHashSet<>();
                for (String module : modules) {
                    File moduleDir = new File(clusterDir, module);
                    String codeNameBase = projectsByDir.get(moduleDir);
                    if (codeNameBase != null) {
                        codeBaseNames.add(codeNameBase);
                    } else {
                        //System.out.println("Cannot identify project at: " + moduleDir.getAbsolutePath());
                    }
                }
                if (!codeBaseNames.isEmpty()) {
                    NbCluster cluster = maybeCreate(clusterName);
                    for (String codeBaseName : codeBaseNames) {
                        cluster.project(codeBaseName);
                    }
                }
            }
        }
        for (String propName : props.stringPropertyNames()) {
            /*
            if (propName.startsWith(CONFIG_PREFIX) && propName.endsWith(LIST_SUFFIX)) {
                String configName = propName.substring(CONFIG_PREFIX.length(), propName.length() - LIST_SUFFIX.length());
                Set<NbCluster> clusterList = new LinkedHashSet<>();
                for (String name : props.getProperty(propName).split(",")) {
                    String cName = name.trim().substring(CLUSTER_PREFIX.length());
                    NbCluster cluster = ret.clusters.get(cName);
                    if (cluster != null) {
                        clusterList.add(cluster);
                    } else {
                        System.err.println("Missing cluster definition: " + cName);
                    }
                }
                ret.clusterConfigs.put(configName, clusterList);
            }*/
            if (propName.startsWith(CLUSTER_PREFIX) && propName.endsWith(DEPENDS_SUFFIX)) {
                String clusterName = propName.substring(CLUSTER_PREFIX.length(), propName.length() - DEPENDS_SUFFIX.length());
                NbCluster cluster = findByName(clusterName);
                if (cluster != null) {
                    for (String name : props.getProperty(propName).split(",")) {
                        if (name.startsWith(CLUSTER_PREFIX)) {
                            String cName = name.substring(CLUSTER_PREFIX.length());
                            NbCluster dep = findByName(cName);
                            if (dep != null) {
                                cluster.dependsOn(dep);
                            } else {
                                System.err.println("Missing cluster definition: " + cName);
                            }
                        }
                    }
                } else {
                    System.err.println("Missing cluster definition: " + clusterName);
                }
            }
        }
    }

    @Override
    public FileCollection getFinalDirs() {
        ArrayList<String> relPaths = new ArrayList<>();
        for (NbCluster cluster : this) {
            relPaths.add("build/netbeans/" + cluster.getName());
        }
        return project.files((Object[]) relPaths.toArray(new String[relPaths.size()]));
    }

}
