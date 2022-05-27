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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.tasks.Jar;
import org.gradle.tooling.BuildException;

import static org.netbeans.gradle.nbsupport.nb.support.NbProjectExtension.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lkishalmi
 */
public class JavaModulePlugin implements Plugin<Project> {

    private final ObjectFactory objects;
    private static final Logger LOG = LoggerFactory.getLogger(JavaModulePlugin.class);

    @Inject
    public JavaModulePlugin(ObjectFactory objects) {
        this.objects = objects;
    }

    @Override
    public void apply(Project project) {

        PluginManager plugins = project.getPluginManager();

        plugins.apply(JavaPlugin.class);

        NbProjectExtension nbproject = project.getExtensions().getByType(NbProjectExtension.class);
        NbBuildExtension nbbuild = project.getExtensions().getByType(NbBuildExtension.class);

        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        //java.setSourceCompatibility(JavaVersion.toVersion(nbproject.getProperty("javac.source")));
        java.setSourceCompatibility(JavaVersion.VERSION_1_8);

        SourceSet main = java.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        prepareTestConfiguration(project, nbproject);

        prepareSourceSets(project);
        updateCompileTasks(project);

        TaskProvider<Task> copyExternals = NetBeansModulePlugin.registerCopyExternals(project);
        
        List<TaskProvider<?>> externals = NetBeansModulePlugin.createCopyExternals(project, nbbuild, nbproject, copyExternals);
//        List<TaskProvider<?>> externals = createCopyExternals(project, nbbuild, nbproject, null);

        if (!nbproject.isTestOnly()) {
            updateJarTask(project, nbproject);
            if (nbproject.getModule().isPureExternalWrapper()) {
               // registerWrappedLibraries(project, nbproject);
            }
            else {
                createPublicPackageJar(project, nbproject);
                createModuleJar(project, main, nbproject);
            }
        }
        
        addNbDependenciesTask(project);

        copyTestData(project);
        project.beforeEvaluate((Project prj) -> beforeEvaluate(prj));
        project.afterEvaluate((Project prj) -> afterEvaluate(prj));
        
        /*
        String moduleName = nbproject.module.getCodeNameBase();
        moduleName = nbproject.isTestOnly() ? moduleName.substring(0, moduleName.length() - 5) : moduleName;
        if (!"nbbuild".equals(moduleName)) {
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
        }*/
    }

    private void beforeEvaluate(Project project) {
        prepareDependencies(project);
    }

    private void afterEvaluate(Project project) {
        NbProjectExtension nbproject = project.getExtensions().getByType(NbProjectExtension.class);        
        project.getTasks().getByName("assemble").dependsOn("copyExternals");
        
        updateTestTask(project);

    }
    
    private void addNbDependenciesTask(Project prj) {
        prj.getTasks().register("nbDependencies", (Task task) -> {
           task.doLast((t) -> {
               try {
                   NbProjectExtension nbproject = prj.getExtensions().getByType(NbProjectExtension.class);
                   nbproject.inspectDependencies(System.out);
               } catch (IOException ex) {
               }
           });
        });
    }

    private void prepareSourceSets(Project prj) {
        SourceSetContainer ss = prj.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = ss.getByName(SourceSet.MAIN_SOURCE_SET_NAME);


        Configuration apiElements = prj.getConfigurations().findByName(main.getApiElementsConfigurationName());
        apiElements.getArtifacts().clear();

//        Configuration runtimeElements = prj.getConfigurations().findByName(main.getRuntimeElementsConfigurationName());
//        runtimeElements.getArtifacts().clear();

        File srcDir = new File(prj.getProjectDir(), "src");
        main.getJava().setSrcDirs(Collections.singleton(srcDir));
        main.getJava().include("**/*.java");
        main.getResources().setSrcDirs(Collections.singleton(srcDir));
        main.getResources().exclude("**/*.java", "**/doc-files", "**/package.html");

        SourceSet test = ss.getByName(SourceSet.TEST_SOURCE_SET_NAME);
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
            jar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
            jar.getDestinationDirectory().set(new File(nbprj.getTestDestBaseDir("unit"), prj.getName().replace('.', '-')));
            jar.getArchiveFileName().set("tests.jar");
            jar.dependsOn("testClasses");
            jar.from(test.getOutput());
            jar.getArchiveClassifier().set("test");
        });
        prj.getArtifacts().add("testApi", pvd);
    }

    private static void addDependency(Project prj, String targetConfiguration, NbModule.Dependency dependency) {
        addDependency(prj, targetConfiguration, dependency, null);
    }

    private static void addDependency(Project prj, String targetConfiguration, NbModule.Dependency dependency, String sourceConfiguration) {
        DependencyHandler dh = prj.getDependencies();
        Project dprj = getProjectByCodeNameBase(prj, dependency.codeNameBase);
        if (sourceConfiguration != null) {
            dh.add(targetConfiguration, dh.project(Map.of("path", dprj.getPath(), "configuration", "runtimeElements")));
        } else {
            dh.add(targetConfiguration, dprj);
        }
    }

    private static String dependencyKey(NbModule.Dependency dependency, String sourceConfiguration) {
        return sourceConfiguration != null ? dependency.codeNameBase + ":" + sourceConfiguration : dependency.codeNameBase;
    }

    private static void addRecursiveDependency(Project prj, Set<String> visited, String targetConfiguration, NbModule.Dependency dependency, String sourceConfiguration) {
        Project dprj = getProjectByCodeNameBase(prj, dependency.codeNameBase);
        NbModule dmodule = dprj.getExtensions().getByType(NbProjectExtension.class).getModule();
        for (String string : dependenc) {

        }
    }

    private void prepareProcessorDependencies(Project prj, NbProjectExtension nbproject, NbBuildExtension nbbuild) {
        NbModule module = nbproject.getModule();
        if (!nbproject.isTestOnly()) {
            for (NbModule.Dependency dep: module.dependencies.get(NbModule.DependencyType.MAIN)) {
                if (dep.compileDependency) {
                    continue;
                }
                addDependency(prj, "annotationProcessor", dep);
                if (dep.)
            }
        }
    }

    private void prepareDependencies(Project prj) {
        DependencyHandler dh = prj.getDependencies();
        NbBuildExtension nbbuild = prj.getExtensions().getByType(NbBuildExtension.class);
        NbProjectExtension nbProject = prj.getExtensions().getByType(NbProjectExtension.class);
        NbModule module = nbProject.getModule();
        if (module == null) {
            throw new BuildException("" + prj + " does not have module", new NullPointerException());
        }

        if (!nbProject.isTestOnly()) {
            for (NbModule.Dependency dependency : module.getDependencies(NbModule.DependencyType.MAIN)) {
                Project dprj = getProjectByCodeNameBase(prj, dependency.codeNameBase);
                NbModule dmodule = dprj.getExtensions().getByType(NbProjectExtension.class).getModule();
                if (dependency.compileDependency) {
                    if (dependency.implementationVersion) {
                        dh.add("implementation", dh.project(Map.of("path", dprj.getPath(), "configuration", "runtimeElements")));
                    } else if(dmodule.isFriend(module)) {
                        dh.add("compileOnly", dprj);
                        if (dependency.buildRequisite || nbbuild.getAnnotationProcessors().contains(dependency.codeNameBase)) {
                            dh.add("annotationProcessor", dh.project(Map.of("path", dprj.getPath(), "configuration", "runtimeElements")));
                            dh.add("testAnnotationProcessor", dh.project(Map.of("path", dprj.getPath(), "configuration", "runtimeElements")));
                        }
                    } else {
                        if (!dmodule.friendPackages.isEmpty()) {
                            throw new BuildException("Project " + prj.getPath() + " is not friend of " + dprj.getPath(), null);
                        }
                        if (dmodule.publicPackages.isEmpty() && !dmodule.isPureExternalWrapper()) {
                            LOG.warn("Project " + prj.getPath() + " has a compile dependency on a non-API module " + dprj.getPath());
                        }
                    }
                }
            }
        }

        if (nbProject.isTestOnly()) {
            Project dprj = getProjectByCodeNameBase(prj, module.codeNameBase);
            dh.add("testImplementation", dprj);
            dh.add("testAnnotationProcessor", dprj);
        } else {
            Jar jar = (Jar) prj.getTasks().findByName("jar");
            dh.add("testAnnotationProcessor", prj.files(jar.getArchiveFile()));
        }

        Set<? extends NbModule.Dependency> unitTestDeps = module.getDependencies(NbModule.DependencyType.TEST_UNIT);
        for (NbModule.Dependency dependency : unitTestDeps) {
            if (dependency.test) {
                Project dprj = hasProjectByCodeNameBase(prj, dependency.codeNameBase + "-test")
                        ? getProjectByCodeNameBase(prj, dependency.codeNameBase + "-test")
                        : getProjectByCodeNameBase(prj, dependency.codeNameBase);
                dh.add("testImplementation", dh.project(Map.of("path", dprj.getPath(), "configuration", "testApi")));
            }
            if ( hasProjectByCodeNameBase(prj, dependency.codeNameBase) ) {
                Project dprj = getProjectByCodeNameBase(prj, dependency.codeNameBase);
                dh.add("testImplementation", dprj);
                if (nbbuild.getAnnotationProcessors().contains(dependency.codeNameBase)) {
                    dh.add("testAnnotationProcessor", dprj);
                }
            } else {
                System.out.println("No dependency for " + dependency.codeNameBase + " in " + prj.getName());
            }
        }
    }

    private void registerWrappedLibraries(Project prj, NbProjectExtension nbproject) {
        ArtifactHandler artifacts = prj.getArtifacts();
        nbproject.getModule().classPathExtensions.forEach((ext) -> {
            File target = new File(nbproject.getModuleDestDir(), ext.runtimeRelativePath);
            PublishArtifact wrappedJar = artifacts.add("apiElements", target, (art) -> {
                art.builtBy(NetBeansModulePlugin.copyExternalTaskName(target));
            });
            prj.getConfigurations().maybeCreate("runtimeElements").getArtifacts().add(wrappedJar);
        });

    }


    private void createPublicPackageJar(Project prj, NbProjectExtension nbproject) {
        NbModule module = nbproject.getModule();

        SourceSetContainer sourceSets = prj.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        ArtifactHandler artifacts = prj.getArtifacts();


        List<String> publicPackages = new ArrayList<>();
        publicPackages.addAll(module.publicPackages);
        publicPackages.addAll(module.friendPackages);
        if (!publicPackages.isEmpty() ) {
            TaskProvider<Jar> publicJar = prj.getTasks().register("publicJar", Jar.class, (Jar jar) -> {
                jar.setGroup("build");
                jar.setDescription("Assembles the NetBeans public/friend API jar.");

                jar.dependsOn(main.getClassesTaskName());
                for (NbModule.ClasspathExtension ext : module.classPathExtensions) {
                    if (ext.binaryOrigin.isPresent()) jar.from(prj.zipTree(prj.file(ext.binaryOrigin.get())));
                }
                jar.from(main.getOutput().getClassesDirs());
                jar.include(publicPackages);
                jar.getArchiveFileName().set(prj.getName().replace('.', '-') + "-api.jar");
                jar.getDestinationDirectory().set(new File(prj.getBuildDir(), "libs"));
            });
            artifacts.add(main.getApiElementsConfigurationName(), publicJar, (art) -> {
                art.setName(prj.getName().replace('.', '-'));
                art.setClassifier("api");
                art.setType("jar");
                art.setExtension("jar");
            });
        }
    }

    private void createModuleJar(Project prj, SourceSet main, NbProjectExtension nbproject) {
        Configuration modules = prj.getConfigurations().maybeCreate("modules");
        modules.setCanBeResolved(false);
        modules.setCanBeConsumed(true);

        modules.attributes((AttributeContainer attrs) -> {
            attrs.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
            attrs.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
            attrs.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8);
            attrs.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
            attrs.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, "module-jar"));
        });

        TaskProvider<Jar> moduleJar = prj.getTasks().register("moduleJar", Jar.class, (jar) -> configureModuleJar(jar, main, nbproject));
        prj.getArtifacts().add(modules.getName(), moduleJar, (art) -> {
            art.setName(prj.getName().replace('.', '-'));
            art.setType("jar");
            art.setExtension("jar");
        });
//        prj.getTasks().getByName("assemble").dependsOn(moduleJar);
    }

    private void configureModuleJar(Jar jar, SourceSet main, NbProjectExtension nbproject) {
        Project prj = jar.getProject();
        jar.dependsOn(main.getClassesTaskName());
        jar.dependsOn(main.getProcessResourcesTaskName());
        jar.setGroup("build");
        jar.setDescription("Assembles the NetBeans module jar from the main classes.");

        jar.from(main.getOutput());

        jar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
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
            String moduleDeps = openideModuleModuleDependencies(prj, nbproject.getModule().directMainDependencies);
            if (!moduleDeps.isEmpty()) {
                attrs.put("OpenIDE-Module-Module-Dependencies", moduleDeps);
            }
            attrs.put("OpenIDE-Module-Java-Dependencies", "Java > 1.8");
        }

        boolean showInClient = !nbproject.isAutoLoad() && !nbproject.isEager()
                && "module".equals(nbproject.getProperty(MODULE_JAR_DIR));
        attrs.put("AutoUpdate-Show-In-Client", Boolean.toString(showInClient));

        if (nbproject.getImplementationVersion() == null) {
            attrs.put("OpenIDE-Module-Implementation-Version", prj.getVersion());
        }

        String cp = classPathEntry(nbproject.getModule());
        if (!cp.isEmpty()) {
            attrs.put("Class-Path", cp);
        }
    }

    private void copyTestData(Project prj) {
        TaskProvider pvd = prj.getTasks().register("copyTestData", Copy.class, (Copy copy) -> {
            copy.from(prj.file("test/unit/data")).into(new File(prj.getBuildDir(), "test/unit/data"));
        });
        
        prj.getTasks().named("test").configure((Task task) -> task.dependsOn(pvd));
    }

    private void updateCompileTasks(Project prj) {
        TaskProvider<Task> compileJava = prj.getTasks().named("compileJava");
        compileJava.configure((Task task) -> {
            JavaCompile compile = (JavaCompile) task;
            compile.getOptions().setSourcepath(task.getProject().files("src"));
            compile.doFirst(CompileInfoAction.INSTANCE);
        });
        prj.getTasks().named("compileTestJava").configure((Task task) -> {
            JavaCompile compile = (JavaCompile) task;
            compile.getOptions().setSourcepath(task.getProject().files("test/unit/src"));
        });
    }

    private void updateJarTask(Project prj, NbProjectExtension nbproject) {
        TaskProvider<Task> jarTask = prj.getTasks().named("jar");
        jarTask.configure((task) -> {
            Jar jar = (Jar) task;
            jar.setEnabled(!nbproject.getModule().isPureExternalWrapper());
            jar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
            String jarName = nbproject.getProperty(MODULE_JAR_NAME);
            jarName = jarName != null ? jarName : prj.getName().replace('.', '-') + ".jar";
            jar.getArchiveFileName().set(jarName);
        });
        if (!nbproject.getModule().isPureExternalWrapper()) {
            prj.getArtifacts().add("runtimeElements", jarTask);
        }
    }

    private void updateTestTask(Project prj) {
        prj.getTasks().named("test").configure((task) -> {
            NbProjectExtension nbproject = prj.getExtensions().getByType(NbProjectExtension.class);
            NbClusterContainer clusters = prj.getRootProject().getExtensions().getByType(NbClusterContainer.class);
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
            test.systemProperty("cluster", nbproject.getCluster());
            test.systemProperty("cluster.path.final", clusters.getFinalDirs().getAsPath());
            test.systemProperty("platform.dir", new File(prj.getRootDir(), "build/netbeans/platform").getAbsolutePath());

        });
    }

    private static String openideModulePublicPackages(NbModule module) {
        if (module.publicPackages.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (String pkg : module.publicPackages) {
            sb.append(separator);
            sb.append(pkg);
            separator = ", ";
        }
        return sb.toString();
    }

    private static String openideModuleModuleDependencies(Project prj, Set<? extends NbModule.Dependency> deps) {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (NbModule.Dependency dep : deps) {
            if (dep.runtime) {
                sb.append(separator);
                sb.append(dep.codeNameBase);
                if (dep.releaseVersion.isPresent()&& !dep.releaseVersion.get().isEmpty()) {
                    sb.append('/').append(dep.releaseVersion.get());
                }
                if (dep.implementationVersion) {
                    Project dprj = getProjectByCodeNameBase(prj, dep.codeNameBase);
                    if (dprj != null) {
                        NbProjectExtension dext = dprj.getExtensions().getByType(NbProjectExtension.class);
                        sb.append(" = ").append(dext.getImplementationVersion());
                    }
                } else if (dep.specificationVersion.isPresent()) {
                    sb.append(" > ").append(dep.specificationVersion.get());
                }
                separator = ", ";
            }
        }
        return sb.toString();
    }

    private static Project getProjectByCodeNameBase(Project project, String codeNameBase) {
        NbClusterContainer clusters = project.getRootProject().getExtensions().getByType(NbClusterContainer.class);
        Project ret = clusters.getProjectByCodeName(codeNameBase);
        if (ret == null) {
            throw new BuildException("No project found for: " + codeNameBase, new NullPointerException());
        }
        return ret;
    }

    private static boolean hasProjectByCodeNameBase(Project project, String codeNameBase) {
        NbClusterContainer clusters = project.getRootProject().getExtensions().getByType(NbClusterContainer.class);
        return clusters.getProjectByCodeName(codeNameBase) != null;
    }

    private static String classPathEntry(NbModule module) {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (NbModule.ClasspathExtension cp : module.classPathExtensions) {
            sb.append(separator).append(cp.runtimeRelativePath);
            separator = " ";
        }
        return sb.toString();
    }

    static class CompileInfoAction implements Action<Task> {

        static CompileInfoAction INSTANCE = new CompileInfoAction();

        @Override
        public void execute(Task task) {
            JavaCompile compile = (JavaCompile) task;
            System.out.println("AP: " + compile.getOptions().getAnnotationProcessorPath().getFiles());
            System.out.println("CP: " + compile.getClasspath().getFiles());
        }

    }
}
