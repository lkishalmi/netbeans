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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.internal.JvmPluginsHelper;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;

/**
 *
 * @author lkishalmi
 */
public class NetBeansModulePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java");
        Clusters clusters = (Clusters) project.getRootProject().getExtensions().findByName("clusters");

        NbBuildExtension nbbuild = new NbBuildExtension();
        NbProjectExtension nbproject = new NbProjectExtension(project);
        project.getExtensions().add("nbbuild", nbbuild);
        project.getExtensions().add("nbproject", nbproject);

        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        //java.setSourceCompatibility(JavaVersion.toVersion(nbproject.getProperty("javac.source")));
        java.setSourceCompatibility(JavaVersion.VERSION_1_8);
        prepareTestConfiguration(project);
        project.setDescription(nbproject.getDisplayName());
        prepareSourceSets(project);
        updateCompileTasks(project);
        
        String moduleName = project.getName();
        moduleName = nbproject.isTestOnly() ? moduleName.substring(0, moduleName.length() - 5) : moduleName;
        if (!"nbbuild".equals(moduleName)) {
            NbModule module = clusters.modulesByName.get(moduleName);
            Task copyExt = project.getTasks().create("copyExternals");
            project.afterEvaluate((Project prj) -> {
                copyExternals(prj, nbproject, module);
            });
            copyTestData(project, nbproject, module);
            prepareDependencies(project, nbproject, module);
            if (!nbproject.isTestOnly()) {
                updateJarTask(project, nbproject);
            }
            updateTestTask(project, nbproject);
        }
    }

    private void prepareSourceSets(Project prj) {
        SourceSetContainer ss = prj.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = ss.getByName("main");

        JvmPluginsHelper.addApiToSourceSet(main, prj.getConfigurations());

        File srcDir = new File(prj.getProjectDir(), "src");
        main.getJava().setSrcDirs(Collections.singleton(srcDir));
        main.getJava().include("**/*.java");
        main.getResources().setSrcDirs(Collections.singleton(srcDir));
        main.getResources().exclude("**/*.java", "**/doc-files", "**/package.html");

        SourceSet test = ss.getByName("test");
        File testSrcDir = new File(prj.getProjectDir(), "test/unit/src");
        test.getJava().setSrcDirs(Collections.singleton(testSrcDir));
        test.getJava().include("**/*.java");
        test.getResources().setSrcDirs(Collections.singleton(testSrcDir));
        test.getResources().exclude("**/*.java");
    }

    private void prepareTestConfiguration(Project prj) {
        prj.getConfigurations().create("testApi");

        SourceSetContainer ss = prj.getExtensions().getByType(SourceSetContainer.class);
        SourceSet test = ss.getByName("test");
        Jar jar = prj.getTasks().create("jarTest", Jar.class);
        jar.dependsOn("testClasses");
        jar.from(test.getOutput());
        jar.getArchiveClassifier().set("test");

        prj.getArtifacts().add("testApi", jar);
    }

    private void prepareDependencies(Project prj, NbProjectExtension nbProject, NbModule module) {
        DependencyHandler dh = prj.getDependencies();
        NbBuildExtension nbbuild = prj.getExtensions().getByType(NbBuildExtension.class);

        for (String ext : module.getClassPathExtensions().keySet()) {
            dh.add("api", prj.files(new File(nbProject.getModuleDestDir(), ext)));
        }

        if (!nbProject.isTestOnly()) {
            for (NbModule.Dependency dependency : module.getDependencies(NbModule.DependencyType.MAIN)) {
                Project dprj = prj.findProject(":" + dependency.getCodeNameBase());
                dh.add("implementation", dprj);
                if (dependency.isBuildPrerequisite() || nbbuild.getAnnotationProcessors().contains(dependency.getCodeNameBase())) {
                    dh.add("annotationProcessor", dprj);
                    dh.add("testAnnotationProcessor", dprj);
                }
            }
        }

        if (nbProject.isTestOnly()) {
            Project dprj = prj.findProject(":" + module.getName());
            dh.add("testImplementation", dprj);
            dh.add("testAnnotationProcessor", dprj);
        } else {
            Jar jar = (Jar) prj.getTasks().findByName("jar");
            dh.add("testAnnotationProcessor", prj.files(jar.getArchiveFile()));
        }

        Set<? extends NbModule.Dependency> unitTestDeps = module.getDependencies(NbModule.DependencyType.TEST_UNIT);
        for (NbModule.Dependency dependency : unitTestDeps) {
            if (dependency.isTest()) {
                Project dprj = prj.findProject(":" + dependency.getCodeNameBase() + "-test");
                String ppath = ":" + dependency.getCodeNameBase();
                if (dprj != null) {
                    ppath += "-test";
                }
                Map<String, String> pdep = Map.of("path", ppath, "configuration", "testApi");
                dh.add("testImplementation", dh.project(pdep));
            }
            Project dprj = prj.findProject(":" + dependency.getCodeNameBase());
            dh.add("testImplementation", dprj);
            if (nbbuild.getAnnotationProcessors().contains(dependency.getCodeNameBase())) {
                dh.add("testAnnotationProcessor", dprj);
            }
        }
    }

    private void copyExternals(Project prj, NbProjectExtension nbproject, NbModule module) {
        Task copyExt = prj.getTasks().findByName("copyExternals");
        NbBuildExtension nbbuild = prj.getExtensions().getByType(NbBuildExtension.class);
        if (nbbuild.isGenerateCopyExternals()) {
            for (Map.Entry<String, String> ext : module.getClassPathExtensions().entrySet()) {
                String taskName = "copyExt-" + ext.getKey().replace('/', '_');
                Copy copy = prj.getTasks().create(taskName, Copy.class);
                File srcFile = new File(prj.getProjectDir(), ext.getValue());
                File destFile = new File(nbproject.getModuleDestDir(), ext.getKey());
                copy.from(srcFile.getParentFile()).into(destFile.getParentFile());
                copy.include(srcFile.getName());
                copy.rename(srcFile.getName(), destFile.getName());
                copyExt.dependsOn(copy);
            }
        }
        prj.getTasks().getByName("compileJava").dependsOn(copyExt);
        prj.getTasks().getByName("build").dependsOn(copyExt);
    }

    private void copyTestData(Project prj, NbProjectExtension nbproject, NbModule module) {
        Copy copy = prj.getTasks().create("copyTestData", Copy.class);
        copy.from(prj.file("test/unit/data")).into(new File(prj.getBuildDir(), "test/unit/data"));
        prj.getTasks().findByName("test").dependsOn(copy);
    }

    private void updateJarTask(Project prj, NbProjectExtension nbproject) {
        Jar jar = (Jar) prj.getTasks().findByName("jar");
        if (jar != null) {
            jar.getDestinationDirectory().set(nbproject.getModuleDestDir());
            String jarName = nbproject.getProperty(NbProjectExtension.MODULE_JAR_NAME);
            jarName = jarName != null ? jarName : prj.getName().replace('.', '-') + ".jar";
            jar.getArchiveFileName().set(jarName);
            jar.getManifest().from(new File(nbproject.getMainProjectDir(), "manifest.mf"));
            jar.getMetaInf().from(prj.getRootDir(), (CopySpec spec) -> {
                spec.include("NOTICE", "LICENSE");
            });
        }
        jar.setEnabled(prj.file("src").isDirectory());
    }

    private void updateCompileTasks(Project prj) {
        JavaCompile compile = (JavaCompile) prj.getTasks().findByName("compileJava");
        compile.getOptions().setSourcepath(prj.files("src"));
        compile = (JavaCompile) prj.getTasks().findByName("compileTestJava");
        compile.getOptions().setSourcepath(prj.files("test/unit/src"));
    }

    private void updateTestTask(Project prj, NbProjectExtension nbproject) {
        Test test = (Test) prj.getTasks().findByName("test");
        String[] includes = nbproject.getProperty(NbProjectExtension.TEST_INCLUDES).split(",");
        test.include(includes);
        if (nbproject.getProperty(NbProjectExtension.TEST_EXCLUDES) != null) {
            String[] excludes = nbproject.getProperty(NbProjectExtension.TEST_EXCLUDES).split(",");
            test.exclude(excludes);
        }
        for (Map.Entry<String, String> prop : nbproject.getTestProperties("unit").entrySet()) {
            test.systemProperty(prop.getKey(), prop.getValue());
        }
        test.systemProperty("xtest.data", new File(prj.getBuildDir(), "test/unit/data").getAbsolutePath());
        test.systemProperty("nbjunit.workdir", new File(prj.getBuildDir(), "test/unit/work").getAbsolutePath());
        test.systemProperty("nbjunit.hard.timeout", "600000");
    }
}
