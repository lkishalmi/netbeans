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
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.CopySpec;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.internal.JvmPluginsHelper;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;

import static org.netbeans.gradle.nbsupport.nb.support.NbProjectExtension.*;

/**
 *
 * @author lkishalmi
 */
public class NetBeansModulePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply("java");

        NbProjectExtension nbproject = project.getExtensions().getByType(NbProjectExtension.class);
        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        //java.setSourceCompatibility(JavaVersion.toVersion(nbproject.getProperty("javac.source")));
        java.setSourceCompatibility(JavaVersion.VERSION_1_8);
        prepareTestConfiguration(project, nbproject);
        project.setDescription(nbproject.getDisplayName());
        prepareSourceSets(project);
        updateCompileTasks(project);
        
        String moduleName = project.getName();
        moduleName = nbproject.isTestOnly() ? moduleName.substring(0, moduleName.length() - 5) : moduleName;
        if (!"nbbuild".equals(moduleName)) {
            project.getTasks().register("copyExternals");
            project.afterEvaluate((Project prj) -> {
                copyExternals(prj);
                if (!nbproject.isTestOnly()) {
                    updateJarTask(prj);
                }
                updateTestTask(prj);
            });
            prepareDependencies(project);
            addNbDependenciesTask(project);
            copyTestData(project);
        }
    }

    private void addNbDependenciesTask(Project prj) {
        prj.getTasks().register("nbDependencies", (Task task) -> {
           task.doLast((t) -> {
               try {
                   NbProjectExtension nbproject = prj.getExtensions().getByType(NbProjectExtension.class);
                   nbproject.getModule().inspectDependencies(System.out);
               } catch (IOException ex) {
               }
           });
        });
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

    private void prepareTestConfiguration(Project prj, NbProjectExtension nbprj) {
        prj.getConfigurations().create("testApi");

        SourceSetContainer ss = prj.getExtensions().getByType(SourceSetContainer.class);
        SourceSet test = ss.getByName("test");
        TaskProvider<Jar> pvd = prj.getTasks().register("jarTest", Jar.class, (Jar jar) -> {
            jar.getDestinationDirectory().set(new File(nbprj.getTestDestBaseDir("unit"), prj.getName().replace('.', '-')));
            jar.getArchiveFileName().set("tests.jar");
            jar.dependsOn("testClasses");
            jar.from(test.getOutput());
            jar.getArchiveClassifier().set("test");
        });
        prj.getArtifacts().add("testApi", pvd);
    }

    private void prepareDependencies(Project prj) {
        DependencyHandler dh = prj.getDependencies();
        NbBuildExtension nbbuild = prj.getExtensions().getByType(NbBuildExtension.class);
        NbProjectExtension nbProject = prj.getExtensions().getByType(NbProjectExtension.class);
        NbModule module = nbProject.getModule();

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
            Project dprj = prj.findProject(":" + module.getCodeNameBase());
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

    private void copyExternals(Project prj) {
        NbBuildExtension nbbuild = prj.getExtensions().getByType(NbBuildExtension.class);
        NbProjectExtension nbproject = prj.getExtensions().getByType(NbProjectExtension.class);
        List<TaskProvider> externals = new LinkedList<>();
        if (nbbuild.isGenerateCopyExternals()) {
            for (Map.Entry<String, String> ext : nbproject.getModule().getClassPathExtensions().entrySet()) {
                if (ext.getValue() != null) {
                    String taskName = "copyExt-" + ext.getKey().replace('/', '_');
                    externals.add(prj.getTasks().register(taskName, Copy.class, (Copy copy) -> {
                        File srcFile = new File(prj.getProjectDir(), ext.getValue());
                        File destFile = new File(nbproject.getModuleDestDir(), ext.getKey());
                        copy.from(srcFile.getParentFile()).into(destFile.getParentFile());
                        copy.include(srcFile.getName());
                        copy.rename(srcFile.getName(), destFile.getName());
                    }));
                }
            }
        }
        TaskProvider copyExt = prj.getTasks().named("copyExternals",(Task copy) -> {
            for (TaskProvider ext : externals) {
                copy.dependsOn(ext);
            }
        });
        prj.getTasks().named("compileJava").configure((Task task) -> task.dependsOn(copyExt));
    }

    private void copyTestData(Project prj) {
        TaskProvider pvd = prj.getTasks().register("copyTestData", Copy.class, (Copy copy) -> {
            copy.from(prj.file("test/unit/data")).into(new File(prj.getBuildDir(), "test/unit/data"));
        });
        
        prj.getTasks().named("test").configure((Task task) -> task.dependsOn(pvd));
    }

    private void updateJarTask(final Project prj) {
        TaskProvider pvd = prj.getTasks().named("jar");
        if (pvd == null) return;
        pvd.configure((task) -> {
            NbProjectExtension nbproject = prj.getExtensions().getByType(NbProjectExtension.class);
            Jar jar = (Jar) task;
            jar.getDestinationDirectory().set(nbproject.getModuleDestDir());
            String jarName = nbproject.getProperty(MODULE_JAR_NAME);
            jarName = jarName != null ? jarName : prj.getName().replace('.', '-') + ".jar";
            jar.getArchiveFileName().set(jarName);
            jar.getManifest().from(new File(nbproject.getMainProjectDir(), "manifest.mf"));
            jar.getMetaInf().from(prj.getRootDir(), (CopySpec spec) -> {
                spec.include("NOTICE", "LICENSE");
            });

            Attributes attrs = jar.getManifest().getAttributes();
            if (nbproject.isOsgiMode()) {
                attrs.put("Bundle-ManifestVersion", "2");
                prj.getLogger().info("OSGI Support is weak for: " + prj.getName());
            } else if (!"lib".equals(nbproject.getProperty(MODULE_JAR_DIR))) {
                String key = "OpenIDE-Module-Requires";
                String token = "org.openide.modules.ModuleFormat1";
                String requires = nbproject.getManifest().getMainAttributes().getValue(key);
                String newRequires;
                if (requires != null) {
                    newRequires = requires + ", " + token;
                } else {
                    newRequires = token;
                }
                attrs.put(key, newRequires);
            }

            if (!nbproject.isOsgiMode()) {
                attrs.put("OpenIDE-Module-Public-Packages", openideModulePublicPackages(nbproject.getModule()));
                attrs.put("OpenIDE-Module-Module-Dependencies", openideModuleModuleDependencies(prj, nbproject.getModule().getDirectMainDependencies()));
                attrs.put("OpenIDE-Module-Java-Dependencies", "Java > 1.8");
            }

            boolean showInClient = !nbproject.isAutoLoad() && !nbproject.isEager()
                    && "module".equals(nbproject.getProperty(MODULE_JAR_DIR));
            attrs.put("AutoUpdate-Show-In-Client", Boolean.toString(showInClient));

            if (nbproject.getImplementationVersion() == null) {
                attrs.put("OpenIDE-Module-Implementation-Version", jar.getProject().getVersion());
            }

            String cp = classPathEntry(nbproject.getModule());
            if (!cp.isEmpty()) {
                attrs.put("Class-Path", cp);
            }
            jar.setEnabled(jar.getProject().file("src").isDirectory());
        });
    }

    private void updateCompileTasks(Project prj) {
        prj.getTasks().named("compileJava").configure((Task task) -> {
            JavaCompile compile = (JavaCompile) task;
            compile.getOptions().setSourcepath(task.getProject().files("src"));
        });
        prj.getTasks().named("compileTestJava").configure((Task task) -> {
            JavaCompile compile = (JavaCompile) task;
            compile.getOptions().setSourcepath(task.getProject().files("test/unit/src"));
        });
    }

    private void updateTestTask(Project prj) {
        prj.getTasks().named("test").configure((task) -> {
            NbProjectExtension nbproject = prj.getExtensions().getByType(NbProjectExtension.class);
            Test test = (Test) task;
            String[] includes = nbproject.getProperty(TEST_INCLUDES).split(",");
            test.include(includes);
            if (nbproject.getProperty(TEST_EXCLUDES) != null) {
                String[] excludes = nbproject.getProperty(TEST_EXCLUDES).split(",");
                test.exclude(excludes);
            }
            for (Map.Entry<String, String> prop : nbproject.getTestProperties("unit").entrySet()) {
                test.systemProperty(prop.getKey(), prop.getValue());
            }
            test.systemProperty("xtest.data", new File(prj.getBuildDir(), "test/unit/data").getAbsolutePath());
            test.systemProperty("nbjunit.workdir", new File(prj.getBuildDir(), "test/unit/work").getAbsolutePath());
            test.systemProperty("nbjunit.hard.timeout", "600000");

        });
    }

    private static String openideModulePublicPackages(NbModule module) {
        if (module.getPublicPackages().isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (String pkg : module.getPublicPackages()) {
            sb.append(separator);
            sb.append(pkg).append(".*");
            separator = ", ";
        }
        return sb.toString();
    }

    private static String openideModuleModuleDependencies(Project prj, Set<? extends NbModule.Dependency> deps) {
        if (deps.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (NbModule.Dependency dep : deps) {
            sb.append(separator);
            sb.append(dep.getCodeNameBase());
            if (dep.getReleaseVersion() != null && !dep.getReleaseVersion().isEmpty()) {
                sb.append('/').append(dep.getReleaseVersion());
            }
            if (dep.isImplementationVersion()) {
                Project dprj = prj.project(":" + dep.getCodeNameBase());
                if (dprj != null) {
                    NbProjectExtension dext = dprj.getExtensions().getByType(NbProjectExtension.class);
                    sb.append(" = ").append(dext.getImplementationVersion());
                }
            } else {
                sb.append(" > ").append(dep.getSpecificationVersion());
            }
            separator = ", ";
        }
        return sb.toString();
    }

    private static String classPathEntry(NbModule module) {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (String cp : module.getClassPathExtensions().keySet()) {
            sb.append(separator).append(cp);
            separator = " ";
        }
        return sb.toString();
    }
}
