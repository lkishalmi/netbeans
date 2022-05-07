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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author lkishalmi
 */
public final class NbModule {

    public enum DependencyType { MAIN, TEST_UNIT }

    final String codeNameBase;
    final Set<ClasspathExtension> classPathExtensions;
    final List<String> publicPackages;
    final List<String> friendPackages;
    final Set<String> friendModules;
    final Set<Dependency> directMainDependencies;
    final Map<String, Set<Dependency>> directTestDependencies = new HashMap<>();
    final Map<DependencyType, Set<Dependency>> depCache = new EnumMap(DependencyType.class);

    NbModule(String codeNameBase, Set<ClasspathExtension> classPathExtensions, List<String> publicPackages, List<String> friendPackages, Set<String> friendModules, Set<Dependency> directMainDependencies) {
        this.codeNameBase = codeNameBase;
        this.classPathExtensions = createSet(classPathExtensions);
        this.publicPackages = createList(publicPackages);
        this.friendPackages = createList(friendPackages);
        this.friendModules = createSet(friendModules);
        this.directMainDependencies = createSet(directMainDependencies);
    }


    public boolean isFriend(NbModule friend) {
        return !publicPackages.isEmpty() || friendModules.contains(friend.codeNameBase);
    }

    public boolean isPureExternalWrapper() {
        return getDependencies(DependencyType.MAIN).isEmpty()
                && getDependencies(DependencyType.TEST_UNIT).isEmpty()
                && publicPackages.isEmpty()
                && friendPackages.isEmpty()
                && !classPathExtensions.isEmpty();
    }

    Set<Dependency> getDependencies(DependencyType type) {
        Set<Dependency> ret = depCache.get(type);
        if (ret == null) {
            ret = new LinkedHashSet<>();
            Set<? extends Dependency> directDeps;
            switch (type) {
                case TEST_UNIT:
                    directDeps = getDirectTestDependencies("unit");
                    break;
                default:
                    directDeps = directMainDependencies;
            }
            for (Dependency dep: directDeps) {
                ret.add(dep);
/*                if (!dep.getCodeNameBase().equals(getCodeNameBase()) && dep.isRecursive()) {

                    NbModule m = findOrLoadModule(dep.getCodeNameBase());
                    if (m != null) {
                        Set<Dependency> mdeps = m.getDependencies(dep.isTest() ? DependencyType.TEST_UNIT : DependencyType.MAIN);
                        ret.addAll(mdeps);
                    } else {
                        throw new IllegalStateException("No module '" + dep.getCodeNameBase() + "' as a depencency of: " + getCodeNameBase());
                    }
                }*/
            }
            ret = !ret.isEmpty() ? ret : Collections.emptySet();
            depCache.put(type, ret);
        }
        return ret;
    }

    Set<? extends Dependency> getDirectTestDependencies(String testType) {
        Set<Dependency> deps = directTestDependencies.get(testType);
        return deps != null ? deps : Collections.emptySet();
    }

    private static <T> Set<T> createSet(Set<T> origin) {
        if (origin == null) {
            return Collections.emptySet();
        } else {
            switch(origin.size()) {
                case 0: return Collections.emptySet();
                case 1: return Collections.singleton(origin.iterator().next());
                default: return Collections.unmodifiableSet(origin);
            }
        }
    }

    private static <T> List<T> createList(List<T> origin) {
        if (origin == null) {
            return Collections.emptyList();
        } else {
            switch(origin.size()) {
                case 0: return Collections.emptyList();
                case 1: return Collections.singletonList(origin.iterator().next());
                default: return Collections.unmodifiableList(origin);
            }
        }
    }

    public static final class ClasspathExtension {
        public final String runtimeRelativePath;
        public final Optional<String> binaryOrigin;

        public ClasspathExtension(String runtimeRelativePath, String binaryOrigin) {
            this.runtimeRelativePath = runtimeRelativePath;
            this.binaryOrigin = Optional.ofNullable(binaryOrigin);
        }

    }

    public static final class Dependency {
        public final String codeNameBase;
        public final boolean buildRequisite;
        public final boolean runtime;
        public final boolean compileDependency;
        public final boolean recursive;
        public final boolean test;
        public final boolean implementationVersion;
        public final Optional<String> releaseVersion;
        public final Optional<String> specificationVersion;

        public Dependency(String codeNameBase, boolean buildRequisite, boolean runtime, boolean compileDependency, boolean recursive, boolean test, boolean implementationVersion, Optional<String> releaseVersion, Optional<String> specificationVersion) {
            this.codeNameBase = codeNameBase;
            this.buildRequisite = buildRequisite;
            this.runtime = runtime;
            this.compileDependency = compileDependency;
            this.recursive = recursive;
            this.test = test;
            this.implementationVersion = implementationVersion;
            this.releaseVersion = releaseVersion;
            this.specificationVersion = specificationVersion;
        }


    }
    
}
