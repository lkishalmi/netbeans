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

package org.netbeans.modules.gradle.java.api;

import org.netbeans.modules.gradle.spi.Utils;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import static org.openide.util.NbBundle.Messages;

public class GradleJavaSourceSet implements Serializable {

    @Messages({
        "LBL_JAVA=Java",
        "LBL_GROOVY=Groovy",
        "LBL_SCALA=Scala",
        "LBL_RESOURCES=Resources"
    })
    public static enum SourceType {

        JAVA, GROOVY, SCALA, RESOURCES;

        @Override
        public String toString() {
            switch (this) {
                case JAVA: return Bundle.LBL_JAVA();
                case GROOVY: return Bundle.LBL_GROOVY();
                case SCALA: return Bundle.LBL_SCALA();
                default: return Bundle.LBL_RESOURCES();
            }
        }
    }

    public static enum ClassPathType {

        COMPILE, RUNTIME
    }

    public static final String MAIN_SOURCESET_NAME = "main"; //NOI18N
    public static final String TEST_SOURCESET_NAME = "test"; //NOI18N

    Map<SourceType, Set<File>> sources = new EnumMap<>(SourceType.class);
    String name;
    String runtimeConfigurationName;
    String compileConfigurationName;
    String sourcesCompatibility;
    String targetCompatibility;
    boolean testSourceSet;
    Set<File> outputClassDirs;
    File outputResources;
    //Add silent support for webapp docroot.
    File webApp;
    Set<File> compileClassPath;
    Set<File> runtimeClassPath;
    Set<GradleJavaSourceSet> sourceDependencies = Collections.emptySet();

    public GradleJavaSourceSet(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isTestSourceSet() {
        return testSourceSet;
    }

    public String getSourcesCompatibility() {
        return sourcesCompatibility;
    }

    public String getTargetCompatibility() {
        return targetCompatibility;
    }

    public String getRuntimeConfigurationName() {
        return runtimeConfigurationName;
    }

    public String getCompileConfigurationName() {
        return compileConfigurationName;
    }

    public Set<File> getSourceDirs(SourceType type) {
        Set<File> ret = sources.get(type);
        return ret != null ? ret : Collections.<File>emptySet();
    }

    public final Set<File> getJavaDirs() {
        return getSourceDirs(SourceType.JAVA);
    }

    public final Set<File> getGroovyDirs() {
        return getSourceDirs(SourceType.GROOVY);
    }

    public final Set<File> getScalaDirs() {
        return getSourceDirs(SourceType.SCALA);
    }

    public final Set<File> getResourcesDirs() {
        return getSourceDirs(SourceType.RESOURCES);
    }

    /**
     * Returns all possible configured source directories regardless
     * of their existence.
     *
     * @param deduplicate 
     * @return all possible source directories.
     */
    public final Collection<File> getAllDirs(boolean deduplicate) {
        Collection<File> ret = deduplicate ? new HashSet<>() : new ArrayList<>();
        if (sources != null) {
            for (Set<File> s : sources.values()) {
                ret.addAll(s);
            }
        }
        return ret;
    }

    public final Collection<File> getAllDirs() {
        return getAllDirs(true);
    }

    /**
     * Returns all configured and existing source directories.
     *
     * @return all existing source directories.
     */
    public final Collection<File> getAvailableDirs(boolean deduplicate) {
        Collection<File> ret = deduplicate ? new HashSet<File>() : new ArrayList<File>();
        if (sources != null) {
            for (Set<File> s : sources.values()) {
                for (File f : s) {
                    if (f.isDirectory()) {
                        ret.add(f);
                    }
                }
            }
        }
        return ret;
    }

    public final Collection<File> getAvailableDirs() {
        return getAvailableDirs(true);
    }

    public Set<File> getCompileClassPath() {
        return compileClassPath != null ? compileClassPath : Collections.<File>emptySet();
    }

    public Set<File> getRuntimeClassPath() {
        return runtimeClassPath != null ? runtimeClassPath : getCompileClassPath();
    }

    public SourceType getSourceType(File f) {
        for (SourceType type : sources.keySet()) {
            Set<File> dirs = sources.get(type);
            for (File dir : dirs) {
                if (parentOrSame(f, dir)) {
                    return type;
                }
            }
        }
        return null;
    }

    public boolean hasOverlappingSourceDirs() {
        Set<File> check = new HashSet<>();
        for (SourceType type : SourceType.values()) {
            for (File f : getSourceDirs(type)) {
                if (!check.add(f)) return true;
            }
        }
        return false;
    }

    public Set<SourceType> getSourceTypes(File f) {
        Set<SourceType> ret = EnumSet.noneOf(SourceType.class);
        for (SourceType type : sources.keySet()) {
            Set<File> dirs = sources.get(type);
            for (File dir : dirs) {
                if (parentOrSame(f, dir)) {
                    ret.add(type);
                }
            }
        }
        return ret;
    }

    public boolean outputContains(File f) {
        List<File> checkList = new LinkedList<>(getOutputClassDirs());
        checkList.add(outputResources);
        for (File check : checkList) {
            if (parentOrSame(f, check)) {
                return true;
            }
        }
        return false;
    }

    public Set<File> getOutputClassDirs() {
        return outputClassDirs != null ? outputClassDirs : Collections.<File>emptySet();
    }

    public File getOutputResources() {
        return outputResources;
    }

    /**
     * Returns those SourceSets within this project which output is on our
     * compile/runtime classpath. Most common example is: 'test' SourceSet
     * usually returns the 'main' SourceSet as dependency.
     *
     * @return the in project SourceSet dependencies of this SourceSet.
     */
    public Set<GradleJavaSourceSet> getSourceDependencies() {
        return sourceDependencies;
    }

    /**
     * Returns {@code true} if the given file belongs either to the sources or
     * the outputs of this SourceSet. Due to practical consideration if the
     * project is a war project and this SourceSet is the main SourceSet then
     * the content of the project 'webapp' folder is also associated with this
     * SourceSet.
     *
     * @param f the file to test
     * @return {@code true} if the given file can be associated with this
     *         SourceSet
     */
    public boolean contains(File f) {
        boolean web = (webApp != null) && parentOrSame(f, webApp);
        return web || outputContains(f) || getSourceType(f) != null;
    }

    /**
     * Tries to find a resource given by its relative path name in the
     * directories associated with this SourceSet. The output directories
     * are checked before the source ones.
     * This method returns with the first resource if it is found.
     *
     * @param name the name of the resources, use "/" as separator character.
     * @return the full path of the first resource found or {@code null}
     *         if no such resource can be associated with this SourceSet.
     */
    public File findResource(String name) {
        return findResource(name, true);
    }

    /**
     * Tries to find a resource given by its relative path name in the
     * directories associated with this SourceSet. The output directories
     * are checked before the source ones (if they are included).
     * This method returns with the first resource if it is found.
     *
     * @param name the name of the resources, use "/" as separator character.
     * @param includeOutputs include the outputs (classes and resources) in the search
     * @param types Source types to check, if omitted, all source types will be included.
     * @return the full path of the first resource found or {@code null}
     *         if no such resource can be associated with this SourceSet.
     */
    public File findResource(String name, boolean includeOutputs, SourceType... types) {
        List<File> roots = new ArrayList<>();
        if (includeOutputs) {
            roots.addAll(outputClassDirs);
            roots.add(outputResources);
        }
        SourceType[] checkedRoots = types.length > 0 ? types : SourceType.values();
        for (SourceType checkedRoot : checkedRoots) {
            roots.addAll(getSourceDirs(checkedRoot));
        }
        for (File root : roots) {
            File test = new File(root, name);
            if (test.exists()) {
                return test;
            }
        }
        return null;
    }

    public String relativePath(File f) {
        if (!f.isAbsolute()) return null;
        List<Path> roots = new ArrayList<>();
        for (File dir : getAllDirs()) {
            roots.add(dir.toPath());
        }
        for (File dir : getOutputClassDirs()) {
            roots.add(dir.toPath());
        }
        roots.add(outputResources.toPath());
        Path path = f.toPath();
        for (Path root : roots) {
            if (path.startsWith(root)) {
                return root.relativize(path).toString().replace('\\', '/');
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "JavaSourceSet[" + name + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GradleJavaSourceSet other = (GradleJavaSourceSet) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.sources, other.sources)) {
            return false;
        }
        if (!Objects.equals(this.outputClassDirs, other.outputClassDirs)) {
            return false;
        }
        if (!Objects.equals(this.outputResources, other.outputResources)) {
            return false;
        }
        if (!Objects.equals(this.webApp, other.webApp)) {
            return false;
        }
        if (!Objects.equals(this.compileClassPath, other.compileClassPath)) {
            return false;
        }
        if (!Objects.equals(this.runtimeClassPath, other.runtimeClassPath)) {
            return false;
        }
        return Objects.equals(this.sourceDependencies, other.sourceDependencies);
    }

    static boolean parentOrSame(File f, File supposedParent) {
        if ((f == null) || (supposedParent == null)) {
            return false;
        }
        boolean ret = supposedParent.equals(f);
        File sparent = supposedParent.getParentFile();
        File parent = f;
        while (!ret && (parent != null) && !parent.equals(sparent)) {
            parent = parent.getParentFile();
            ret = supposedParent.equals(parent);
        }
        return ret;
    }

    public String getBuildTaskName(SourceType type) {
        switch (type) {
            case RESOURCES:
                return getProcessResourcesTaskName();
            case JAVA:
                return getCompileTaskName("Java"); //NOI18N
            case GROOVY:
                return getCompileTaskName("Groovy"); //NOI18N
            case SCALA:
                return getCompileTaskName("Scala"); //NOI18N
        }
        return null;
    }

    public String getCompileTaskName(String language) {
        return getTaskName("compile", language);
    }

    public String getProcessResourcesTaskName() {
        return getTaskName("process", "Resources");
    }

    public String getClassesTaskName() {
        return getTaskName("classes", null);
    }

    public String getTaskName(String verb, String target) {
        String n = MAIN_SOURCESET_NAME.equals(name) ? "" : Utils.capitalize(name);
        String t = target == null ? "" : Utils.capitalize(target);
        return verb + n + t;
    }
}
