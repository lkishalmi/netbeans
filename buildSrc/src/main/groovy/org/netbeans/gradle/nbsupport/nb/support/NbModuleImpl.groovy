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

package org.netbeans.gradle.nbsupport.nb.support

import groovy.util.XmlSlurper

/**
 *
 * @author lkishalmi
 */
class NbModuleImpl implements NbModule {

    final NbCluster cluster;
    final String relPath;

    private boolean mainResolved = false;
    private boolean testResolved = false;

    Map<String, String> classPathExtentions = new LinkedHashMap<>();
    Set<? extends NbModule.Dependency> dependencies = new LinkedHashSet<>();
    Map<String, Set<? extends NbModule.Dependency>> testDependencies = new HashMap<>();
    
    String name
    
    public NbModuleImpl(NbCluster cluster, String relPath) {
        this.relPath = relPath;
        this.cluster = cluster;
    }
    
    File getModuleDir() {
        return new File(cluster.dir, relPath);
    }

    String getName() {
        return name;
    }

    Map<String, String> getClassPathExtensions() {
        return classPathExtentions;
    }
    
    Set<? extends NbModule.Dependency> getMainDependencies() {
        if (!mainResolved) {
            LinkedList<? extends NbModule.Dependency> temp = new LinkedList(dependencies);
            mainResolved = true;
            for (NbModule.Dependency dep: temp) {
                if (dep.isRecursive()) {
                    NbModule module = cluster.findInClusters(dep.getCodeNameBase());
                    if (module != null) {
                        dependencies.addAll(module.getMainDependencies())
                    } else {
                        Syetem.err.println("No module '" + dep.getCodeNameBase() + "' found for : '" + name)
                    }
                }
            }
        }
        return dependencies;
    }

    Set<? extends NbModule.Dependency> getTestDependencies(String testType) {
        //TODO: This one works on unittest for now.
        Set<? extends NbModule.Dependency> testDep = testDependencies[testType];
        if ((testDep != null) && !testResolved) {
            LinkedList<? extends NbModule.Dependency> temp = new LinkedList(testDep);
            testResolved = true
            for (NbModule.Dependency dep: temp) {
                if (dep.isRecursive()) {
                    NbModule module = cluster.findInClusters(dep.getCodeNameBase());
                    if (module != null) {
                        def rdeps = dep.isTest() ? module.getTestDependencies(testType) : module.getMainDependencies()
                        //print "Add recursive deps for $name through ${dep.codeNameBase}: ["
                        //rdeps.each {print "${it.codeNameBase} "}
                        //println ']'
                        testDep.addAll(rdeps)
                    } else {
                        Syetem.err.println("No module '" + dep.getCodeNameBase() + "' found for test : '" + name)
                    }
                }
            }
        }
        return testDep != null ? testDep : Collections.emptySet();
    }

    boolean resolve() {
        File projectXML = new File (getModuleDir(), 'nbproject/project.xml')
        if (projectXML.canRead()) {
            def prj = new XmlSlurper().parse(projectXML)
            if ('org.netbeans.modules.apisupport.project'.equals(prj.type.text())) {
                name = prj.configuration.data['code-name-base'].text()
            }
            prj.configuration.data['module-dependencies'].dependency.each { dependency ->
                Dependency dep = new Dependency(dependency['code-name-base'].toString())
                dep.buildPrerequisite = dependency['build-prerequisite'] != null
                dep.compileDependency = dependency['compile-dependency'] != null
                dep.recursive = dependency['recursive'] != null
                if (dependency['run-dependency'] != null) {
                    if (dependency['run-dependency']['release-version'] != null) {
                        dep.releaseVersion = dependency['run-dependency']['release-version']
                    }
                    if (dependency['run-dependency']['specification-version'] != null) {
                        dep.specificationVersion = dependency['run-dependency']['specification-version']
                    }
                }
                dependencies.add(dep)
            }
            prj.configuration.data['test-dependencies']['test-type'].each { testType -> 
                def typeName = testType.name.toString()
                def deps = []
                testType['test-dependency'].each { dependency ->
                    if (dependency['code-name-base'].toString() != name) {
                        Dependency dep = new Dependency(dependency['code-name-base'].toString())
                        dep.buildPrerequisite = !dependency['build-prerequisite'].isEmpty()
                        dep.compileDependency = !dependency['compile-dependency'].isEmpty()
                        dep.recursive = !dependency.recursive.isEmpty()
                        dep.test = !dependency.test.isEmpty()
                        if (!dependency['run-dependency'].empty) {
                            if (!dependency['run-dependency']['release-version'].isEmpty()) {
                                dep.releaseVersion = dependency['run-dependency']['release-version']
                            }
                            if (!dependency['run-dependency']['specification-version'].isEmpty()) {
                                dep.specificationVersion = dependency['run-dependency']['specification-version']
                            }
                        }
                        deps.add(dep)
                    }
                }
                testDependencies[typeName] = deps
            }
           prj.configuration.data['class-path-extension'].each { cpe ->
               String relPath = cpe['runtime-relative-path']
               String origin = cpe['binary-origin']
               classPathExtentions.put(relPath, origin)
           }
        } else {
            return false
        }
        return true
    }

    void printDependencies() {
        println "--- $name dependencies ---"
        println 'Standard:'
        dependencies.each { dep -> println "    $dep" }
        println 'Test:'
        testDependencies.each { k, v ->
            v.each { dep -> println "    $k - $dep" }
        }
    }

    static class Dependency implements NbModule.Dependency {
        final String codeNameBase;
        boolean buildPrerequisite;
        boolean compileDependency;
        boolean recursive;
        boolean test;
        String releaseVersion = '0'
        String specificationVersion = ''
        
        Dependency(String name) {
            codeNameBase = name
        }

        public String getCodeNameBase() {
            return codeNameBase;
        }
        
        String toString() {
            return "$codeNameBase:$releaseVersion:$specificationVersion - test: $test, recursive: $recursive"
        }
    }
}

