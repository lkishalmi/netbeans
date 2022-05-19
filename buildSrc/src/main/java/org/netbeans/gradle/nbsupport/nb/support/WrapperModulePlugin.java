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
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Delete;
import org.gradle.api.tasks.TaskProvider;

/**
 *
 * @author Laszlo Kishalmi
 */
public class WrapperModulePlugin implements Plugin<Project>{

    private final ObjectFactory objects;

    @Inject
    public WrapperModulePlugin(ObjectFactory objects) {
        this.objects = objects;
    }

    @Override
    public void apply(Project project) {
        createConfigurations(project);

        NbProjectExtension nbproject = project.getExtensions().getByType(NbProjectExtension.class);
        NbBuildExtension nbbuild = project.getExtensions().getByType(NbBuildExtension.class);

        TaskProvider<Task> copyExternals = NetBeansModulePlugin.registerCopyExternals(project);

        List<TaskProvider<?>> externals = NetBeansModulePlugin.createCopyExternals(project, nbbuild, nbproject, copyExternals);
        createClean(project, nbproject, nbbuild);
        project.getTasks().register("assemble", (task) -> {
            task.setGroup("build");
            task.dependsOn(copyExternals);
        });
        project.getTasks().register("build", (task) -> {
            task.setGroup("build");
            task.dependsOn("assemble");
        });
    }

    private void createClean(Project project, NbProjectExtension nbproject, NbBuildExtension nbbuild) {
        project.getTasks().register("clean", Delete.class, (task) -> {
            task.setGroup("build");

            NbModule module = nbproject.getModule();
            if (nbbuild.isGenerateCopyExternals()) {
                for (NbModule.ClasspathExtension ext : module.classPathExtensions) {
                    if (!ext.runtimeRelativePath.isEmpty()) {
                        File target = new File(nbproject.getModuleDestDir(), ext.runtimeRelativePath);
                        if (ext.binaryOrigin.isPresent()) {
                            task.delete(target);
                        }
                    }
                }
            }
        });
    }

    private void createConfigurations(Project project) {
        createConfig(project, "apiElements", Usage.JAVA_API);
        createConfig(project, "runtimeElements", Usage.JAVA_RUNTIME);
    }

    private Configuration createConfig(Project project, String name, String usage) {
        Configuration config = project.getConfigurations().maybeCreate(name);
        config.setCanBeResolved(false);
        config.setCanBeConsumed(true);

        config.attributes((AttributeContainer attrs) -> {
            attrs.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
            attrs.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, usage));
            attrs.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8);
            attrs.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
            attrs.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
        });
        return config;
    }
}
