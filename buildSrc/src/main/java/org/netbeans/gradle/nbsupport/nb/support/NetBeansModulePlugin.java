/*
 * Copyright 2022 Laszlo Kishalmi.
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
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskProvider;

/**
 *
 * @author Laszlo Kishalmi
 */
public final class NetBeansModulePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        NbBuildExtension nbbuild = new NbBuildExtension();
        NbProjectExtension nbproject = new NbProjectExtension(project);
        project.getExtensions().add("nbbuild", nbbuild);
        project.getExtensions().add("nbproject", nbproject);

        project.setDescription(nbproject.getDisplayName());

        if (nbproject.getModule().isPureExternalWrapper()) {
            project.getPlugins().apply(WrapperModulePlugin.class);
        } else {
            project.getPlugins().apply(JavaModulePlugin.class);
        }
    }

    static List<TaskProvider<?>> createCopyExternals(Project prj, NbBuildExtension nbbuild, NbProjectExtension nbproject, TaskProvider<Task> copyExternals) {
        ArtifactHandler artifacts = prj.getArtifacts();
        List<TaskProvider<?>> ret = new ArrayList<>();
        NbModule module = nbproject.getModule();
        if (nbbuild.isGenerateCopyExternals()) {
            for (NbModule.ClasspathExtension ext : module.classPathExtensions) {
                if (!ext.runtimeRelativePath.isEmpty()) {
                    File target = new File(nbproject.getModuleDestDir(), ext.runtimeRelativePath);
                    if (ext.binaryOrigin.isPresent()){
                        TaskProvider<Copy> task = copyExternalJar(prj, ext.binaryOrigin.get(), target);
                        if (copyExternals != null) copyExternals.configure((ce) -> ce.dependsOn(task));
                        ret.add(task);

                        PublishArtifact artifact = artifacts.add("runtimeElements", target, (art) -> {
                            art.builtBy(task);
                        });
                        if (module.isPureExternalWrapper()) {
                            Configuration apiElements = prj.getConfigurations().maybeCreate("apiElements");
                            apiElements.getArtifacts().add(artifact);
                        }
                    } else {
                        //System.out.println("Something weird: " + prj.getName() + "Externals: " + nbproject.getModule().classPathExtensions);
                    }
                }
            }
        }
        return ret;
    }

    static TaskProvider<Task> registerCopyExternals(Project prj) {
        TaskProvider<Task> ret = prj.getTasks().register("copyExternals", (task) -> {
            task.setGroup("build");
            task.setDescription("Copy external files into NetBeans");
        });
        return ret;
    }

    static TaskProvider<Copy> copyExternalJar(Project project, String source, File target) {
        String taskName = copyExternalTaskName(target);
        TaskProvider<Copy> task = project.getTasks().register(taskName, Copy.class, (Copy copy) -> {
            File srcFile = new File(project.getProjectDir(), source);
            copy.from(srcFile.getParentFile()).into(target.getParentFile());
            copy.include(srcFile.getName());
            copy.rename(srcFile.getName(), target.getName());

            project.getTasks().getByName("assemble").dependsOn(copy);
        });
        return task;
    }

    static String copyExternalTaskName(File target) {
        return "copyExt-" + target.getName();
    }

}
