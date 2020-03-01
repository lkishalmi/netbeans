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

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author lkishalmi
 */
public abstract class NbModule {

    public enum DependencyType { MAIN, TEST_UNIT }

    Map<DependencyType, Set<Dependency>> depCache = new EnumMap(DependencyType.class);
    final NbCluster cluster;

    NbModule(NbCluster cluster) {
        this.cluster = cluster;
    }

    public abstract String getName();

    abstract Map<String, String> getClassPathExtensions();

    Set<Dependency> getDependencies(DependencyType type) {
        Set<Dependency> ret = depCache.get(type);
        if (ret == null) {
            ret = new LinkedHashSet<Dependency>();
            Set<? extends Dependency> directDeps;
            switch (type) {
                case TEST_UNIT:
                    directDeps = getDirectTestDependencies("unit");
                    break;
                default:
                    directDeps = getDirectMainDependencies();
            }
            for (Dependency dep: directDeps) {
                ret.add(dep);
                if (!dep.getCodeNameBase().equals(getName()) && dep.isRecursive()) {
                    NbModule m = cluster.findInClusters(dep.getCodeNameBase());
                    if (m != null) {
                        Set<Dependency> mdeps = m.getDependencies(dep.isTest() ? DependencyType.TEST_UNIT : DependencyType.MAIN);
                        ret.addAll(mdeps);
                    } else {
                        throw new IllegalStateException("No module '" + dep.getCodeNameBase() + "' as a depencency of: " + getName());
                    }
                }
            }
            ret = !ret.isEmpty() ? ret : Collections.emptySet();
            depCache.put(type, ret);
        }
        return ret;
    }

    abstract Set<? extends Dependency> getDirectMainDependencies();
    abstract Set<? extends Dependency> getDirectTestDependencies(String testType);

    interface Dependency {
        String getCodeNameBase();
        boolean isBuildPrerequisite();
        boolean isCompileDependency();
        boolean isTest();
        boolean isRecursive();
    }
}
