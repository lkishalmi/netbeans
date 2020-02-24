/*
 * Copyright 2018 lkishalmi.
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author lkishalmi
 */
public class Clusters {
    private static final String CONFIG_PREFIX = "clusters.config.";
    private static final String CLUSTER_PREFIX = "nb.cluster.";
    private static final String DIR_SUFFIX = ".dir";
    private static final String DEPENDS_SUFFIX = ".depends";
    private static final String LIST_SUFFIX = ".list";

    Map<String, Set<NbCluster>> clusterConfigs = new HashMap<>();
    Map<String, NbCluster> clusters = new HashMap<>();
    Map<String, NbModule> modulesByName = new HashMap<>();
    
    public static Clusters loadClusters(File rootDir) {
        
        Properties props = new ExpandingProperties();
        Clusters ret = new Clusters();
        try (Reader rd = new FileReader(new File(rootDir, "nbbuild/cluster.properties"))) {
            System.out.println("load clusters");
            props.load(rd);        
        } catch (IOException ex) {}
        
        for (String propName : props.stringPropertyNames()) {
            if (propName.startsWith(CLUSTER_PREFIX) && propName.endsWith(DIR_SUFFIX)) {
                String clusterName = propName.substring(CLUSTER_PREFIX.length(), propName.length() - DIR_SUFFIX.length());
                NbCluster cluster = new NbCluster(ret, clusterName, new File(rootDir, props.getProperty(propName)));
                ret.clusters.put(clusterName, cluster);
                String moduleList = props.getProperty(CLUSTER_PREFIX + clusterName);
                String[] modules = moduleList.split(",");
                for (String module : modules) {
                    NbModuleImpl nbm = new NbModuleImpl(cluster, module.trim());
                    if (nbm.resolve()) {
                        ret.modulesByName.put(nbm.getName(), nbm);        
                        cluster.modules.add(nbm);
                    }
                }
            }
        }
        for (String propName : props.stringPropertyNames()) {
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
            }
            if (propName.startsWith(CLUSTER_PREFIX) && propName.endsWith(DEPENDS_SUFFIX)) {
                String clusterName = propName.substring(CLUSTER_PREFIX.length(), propName.length() - DEPENDS_SUFFIX.length());
                NbCluster cluster = ret.clusters.get(clusterName);
                if (cluster != null) {
                    for (String name : props.getProperty(propName).split(",")) {
                        if (name.startsWith(CLUSTER_PREFIX)) {
                            String cName = name.substring(CLUSTER_PREFIX.length());
                            NbCluster dep = ret.clusters.get(cName);
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
        return ret;
    }
    
    private static class ExpandingProperties extends Properties {

        private static final Pattern REPL = Pattern.compile("\\$\\{([a-zA-Z_0-9.]+)\\}");
        @Override
        public String getProperty(String key) {
            StringBuilder sb = new StringBuilder(super.getProperty(key));
            for (Matcher m; (m = REPL.matcher(sb)).find();) {
                sb.replace(m.start(), m.end(), super.getProperty(m.group(1)));
            }
            return sb.toString();
        }
        
    }
    
    public static void main(String[] args) {
        Clusters cls = Clusters.loadClusters(new File("/home/lkishalmi/NetBeansProjects/incubator-netbeans/nbbuild/cluster.properties"));
        System.out.println("Cluster configs:");
        for (Map.Entry<String, Set<NbCluster>> entry : cls.clusterConfigs.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue().toString());
        }
        System.out.println("Clusters:");
        for (Map.Entry<String, NbCluster> entry : cls.clusters.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue().toString());            
        }
    }
}
