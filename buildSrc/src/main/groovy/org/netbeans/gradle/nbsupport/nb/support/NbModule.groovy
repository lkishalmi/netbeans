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
class NbModule {

    final NbCluster cluster;
    final String relPath;

    def externalDeps = []
    def dependencies = []
    def testDependencies = [:]
    
    String name
    
    public NbModule(NbCluster cluster, String relPath) {
        this.relPath = relPath;
        this.cluster = cluster;
    }
    
    File getModuleDir() {
        return new File(cluster.dir, relPath);
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
                    if (dependency['code-name-base'] != null) {
                        Dependency dep = new Dependency(dependency['code-name-base'].toString())
                        dep.buildPrerequisite = dependency['build-prerequisite'] != null
                        dep.compileDependency = dependency['compile-dependency'] != null
                        dep.recursive = dependency['recursive'] != null
                        dep.test = dependency['test'] != null
                        if (dependency['run-dependency'] != null) {
                            if (dependency['run-dependency']['release-version'] != null) {
                                dep.releaseVersion = dependency['run-dependency']['release-version']
                            }
                            if (dependency['run-dependency']['specification-version'] != null) {
                                dep.specificationVersion = dependency['run-dependency']['specification-version']
                            }
                        }
                        deps.add(dep)
                    }
                }
                testDependencies[typeName] = deps
            }
           
        } else {
            return false
        }
        File externameDependencies = new File(getModuleDir(), 'external/binaries-list')
        if (externameDependencies.canRead()) {
            externameDependencies.readLines().each() { line ->
                if (!line.startsWith('#')) {
                    def dependency = line.substring(line.indexOf(' ') + 1)
                    if (dependency.indexOf(':') > 0) {
                        externalDeps.add(dependency)
                    }
                }
            }
        }
        return true
    }
    
    static class Dependency {
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
    }
}

