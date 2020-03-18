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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.api.Project;

/**
 *
 * @author lkishalmi
 */
public final class NbModule {

    public enum DependencyType { MAIN, TEST_UNIT }

    String codeNameBase;
    Map<String, String> classPathExtensions;
    List<String> publicPackages;
    List<String> friendPackages;
    List<String> friendModules;
    Set<Dependency> directMainDependencies;
    Map<String, Set<Dependency>> directTestDependencies = new HashMap<>();
    Map<DependencyType, Set<Dependency>> depCache = new EnumMap(DependencyType.class);
    final Project project;

    NbModule(Project project) {
        this.project = project;
    }

    public String getCodeNameBase() {
        return codeNameBase;
    }

    public Map<String, String> getClassPathExtensions() {
        return classPathExtensions != null ? classPathExtensions : Collections.emptyMap();
    }

    public List<String> getPublicPackages() {
        return publicPackages != null ? publicPackages : Collections.emptyList();
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
                    directDeps = getDirectMainDependencies();
            }
            for (Dependency dep: directDeps) {
                ret.add(dep);
                if (!dep.getCodeNameBase().equals(getCodeNameBase()) && dep.isRecursive()) {

                    NbModule m = findOrLoadModule(dep.getCodeNameBase());
                    if (m != null) {
                        Set<Dependency> mdeps = m.getDependencies(dep.isTest() ? DependencyType.TEST_UNIT : DependencyType.MAIN);
                        ret.addAll(mdeps);
                    } else {
                        throw new IllegalStateException("No module '" + dep.getCodeNameBase() + "' as a depencency of: " + getCodeNameBase());
                    }
                }
            }
            ret = !ret.isEmpty() ? ret : Collections.emptySet();
            depCache.put(type, ret);
        }
        return ret;
    }

    NbModule findOrLoadModule(String codeNameBase) {
        Project prj = project.findProject(":" + codeNameBase);
        if (prj != null) {
            NbProjectExtension ext = prj.getExtensions().findByType(NbProjectExtension.class);
            if (ext == null) {
                System.out.println("No extension for" + prj.getName());
            }
            return ext.getModule();
        }
        throw new IllegalArgumentException("No project dependency ':" + codeNameBase + "' found for :" + project.getName());
    }

    Set<? extends Dependency> getDirectMainDependencies() {
        return directMainDependencies != null ? directMainDependencies : Collections.emptySet();
    }

    Set<? extends Dependency> getDirectTestDependencies(String testType) {
        Set<Dependency> deps = directTestDependencies.get(testType);
        return deps != null ? deps : Collections.emptySet();
    }

    public void inspectDependencies(PrintStream out) throws IOException {
        for (DependencyType type : DependencyType.values()) {
            inspectDependencies(0, type, out);
            out.println();
        }
    }

    public void inspectDependencies(int level, DependencyType type, PrintStream out) throws IOException {
        Set<? extends Dependency> directDeps;
        switch (type) {
            case TEST_UNIT:
                directDeps = getDirectTestDependencies("unit");
                break;
            default:
                directDeps = getDirectMainDependencies();
        }
        if (level == 0) {
            out.println(codeNameBase + " " + type + " dependencies:");
        }
        for (Dependency dep: directDeps) {
            for (int i = 0; i < level + 1; i++) {
                out.print("  ");
            }
            out.print("- " + dep.getCodeNameBase());
            if (dep.getReleaseVersion() != null) out.print("/" + dep.getReleaseVersion());
            if (dep.isImplementationVersion() || dep.getSpecificationVersion() != null) {
                out.print(dep.isImplementationVersion() ? " = " : " > ");
                out.print(dep.isImplementationVersion() ? "<impl>" : dep.getSpecificationVersion());
            }
            if (dep.isTest()) out.print(" (t)");
            if (dep.isRecursive()) out.print(" (r)");
            out.println();
            if (dep.isRecursive()) {
                NbModule m = findOrLoadModule(dep.getCodeNameBase());
                if (m != null) {
                    m.inspectDependencies(level + 1, dep.isTest() ? DependencyType.TEST_UNIT : DependencyType.MAIN, out);
                } else {
                    throw new IllegalStateException("No module '" + dep.getCodeNameBase() + "' as a depencency of: " + getCodeNameBase());
                }
            }
        }
    }

    public static final class Dependency {
        String codeNameBase;
        boolean buildRequisite;
        boolean runtime;
        boolean compileDependency;
        boolean recursive;
        boolean test;
        boolean implementationVersion;
        String releaseVersion;
        String specificationVersion;

        public String getCodeNameBase() {
            return codeNameBase;
        }

        public boolean isBuildPrerequisite() {
            return buildRequisite;
        }

        public boolean isCompileDependency() {
            return compileDependency;
        }

        public boolean isTest() {
            return test;
        }

        public boolean isRecursive() {
            return recursive;
        }

        public boolean isImplementationVersion() {
            return implementationVersion;
        }

        public boolean isRuntime() {
            return runtime;
        }
        public String getReleaseVersion() {
            return releaseVersion;
        }

        public String getSpecificationVersion() {
            return specificationVersion;
        }
    }
    
}
