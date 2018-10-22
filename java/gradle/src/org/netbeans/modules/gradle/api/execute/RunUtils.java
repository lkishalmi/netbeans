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

package org.netbeans.modules.gradle.api.execute;

import org.netbeans.modules.gradle.api.GradleBaseProject;
import org.netbeans.modules.gradle.api.NbGradleProject;
import org.netbeans.modules.gradle.execute.GradleDaemonExecutor;
import org.netbeans.modules.gradle.execute.GradleExecutor;
import org.netbeans.modules.gradle.execute.ProxyNonSelectableInputOutput;
import org.netbeans.modules.gradle.spi.GradleSettings;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.LifecycleManager;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.ExecutorTask;
import org.openide.util.Lookup;
import org.openide.windows.IOColorPrint;
import org.openide.windows.IOColors;
import org.openide.windows.InputOutput;

import static org.netbeans.modules.gradle.api.execute.GradleCommandLine.Flag.*;
import static org.netbeans.modules.gradle.api.execute.Bundle.*;
import org.netbeans.modules.gradle.spi.actions.ReplaceTokenProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.prefs.Preferences;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;

import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;

/**
 *
 * @author Laszlo Kishalmi
 */
public final class RunUtils {

    private static final Logger LOG = Logger.getLogger(RunUtils.class.getName());
    public static final String PROP_JDK_PLATFORM = "jdkPlatform"; //NOI18N
    public static final String PROP_COMPILE_ON_SAVE = "compile.on.save"; //NOI18N
    public static final String PROP_AUGMENTED_BUILD = "augmented.build"; //NOI18N

    public static FileObject extractFileObjectfromLookup(Lookup lookup) {
        FileObject[] fos = extractFileObjectsfromLookup(lookup);
        return fos.length > 0 ? fos[0] : null;
    }

    public static FileObject[] extractFileObjectsfromLookup(Lookup lookup) {
        List<FileObject> files = new ArrayList<>();
        Iterator<? extends DataObject> it = lookup.lookupAll(DataObject.class).iterator();
        while (it.hasNext()) {
            DataObject d = it.next();
            FileObject f = d.getPrimaryFile();
            files.add(f);
        }
        Collection<? extends SingleMethod> methods = lookup.lookupAll(SingleMethod.class);
        if (methods.size() == 1) {
            SingleMethod method = methods.iterator().next();
            files.add(method.getFile());
        }
        return files.toArray(new FileObject[files.size()]);
    }

       private RunUtils() {
    }


    public static ExecutorTask executeGradle(RunConfig config, String initialOutput) {
        LifecycleManager.getDefault().saveAll();

        GradleExecutor exec = new GradleDaemonExecutor(config);
        ExecutorTask task = executeGradleImpl(config.getTaskDisplayName(), exec, initialOutput);

        return task;
    }

    public static RunConfig createRunConfig(Project project, String action, String displayName, String[] args) {
        GradleCommandLine cmd = new GradleCommandLine(args);
        RunConfig ret = new RunConfig(project, action, displayName, EnumSet.of(RunConfig.ExecFlag.REPEATABLE), cmd);
        return ret;
    }

    private static ExecutorTask executeGradleImpl(String runtimeName, final GradleExecutor exec, String initialOutput) {
        InputOutput io = exec.getInputOutput();
        ExecutorTask task = ExecutionEngine.getDefault().execute(runtimeName, exec,
                new ProxyNonSelectableInputOutput(io));
        if (initialOutput != null) {
            try {
                if (IOColorPrint.isSupported(io)) {
                    IOColorPrint.print(io, initialOutput, IOColors.getColor(io, IOColors.OutputType.LOG_DEBUG));
                } else {
                    io.getOut().println(initialOutput);
                }
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Can't write initial output: " + initialOutput, ex);
            }
        }
        exec.setTask(task);
        return task;
    }

    public static boolean isCompileOnSaveEnabled(Project project) {
        return isOptionEnabled(project, PROP_COMPILE_ON_SAVE, false);
    }

    public static boolean isAugmentedBuildEnabled(Project project) {
        return isOptionEnabled(project, PROP_AUGMENTED_BUILD, true);
    }

    private static boolean isOptionEnabled(Project project, String option, boolean defaultValue) {
        GradleBaseProject gbp = GradleBaseProject.get(project);
        if (gbp != null) {
            String value = gbp.getNetBeansProperty(option);
            if (value != null) {
                return Boolean.getBoolean(value);
            } else {
                return NbGradleProject.getPreferences(project, false).getBoolean(option, defaultValue);
            }
        }
        return false;
    }

    public static void applyGlobalConfig(RunConfig cfg) {
        GradleCommandLine cmd = cfg.getCommandLine();
        GradleSettings settings = GradleSettings.getDefault();
        if (!cmd.hasFlag(OFFLINE)) {
            cmd.setFlag(OFFLINE, settings.isOffline());
        }
        if (!cmd.hasFlag(CONFIGURE_ON_DEMAND)) {
            cmd.setFlag(CONFIGURE_ON_DEMAND, settings.isConfigureOnDemand());
        }
        if (!cmd.hasFlag(NO_REBUILD)) {
            cmd.setFlag(NO_REBUILD, settings.getNoRebuild());
        }

        GradleBaseProject gbp = GradleBaseProject.get(cfg.getProject());

        globalExclude(cmd, gbp, "test", settings.skipTest()); //NOI18N
        globalExclude(cmd, gbp, "check", settings.skipCheck()); //NOI18N

    }

    private static void globalExclude(GradleCommandLine cmd, GradleBaseProject gbp, String task, boolean global) {
        boolean exclude = gbp.getTaskNames().contains(task) || (gbp.isRoot() && !gbp.getSubProjects().isEmpty());
        exclude &= global && !cmd.getTasks().contains(task);
        if (exclude) {
            cmd.addParameter(GradleCommandLine.Parameter.EXCLUDE_TASK, task);
        }
    }

    public static ReplaceTokenProvider simpleReplaceTokenProvider(final String token, final String value) {
        return new ReplaceTokenProvider() {
            @Override
            public Map<String, String> createReplacements(String action, Lookup context) {
                return Collections.singletonMap(token, value);
            }
        };
    }

    @NbBundle.Messages({
        "# {0} - artifactId", "TXT_Run=Run ({0})",
        "# {0} - artifactId", "TXT_Debug=Debug ({0})",
        "# {0} - artifactId", "TXT_ApplyCodeChanges=Apply Code Changes ({0})",
        "# {0} - artifactId", "TXT_Profile=Profile ({0})",
        "# {0} - artifactId", "TXT_Test=Test ({0})",
        "# {0} - artifactId", "TXT_Build=Build ({0})"
    })
    private static String taskName(String action, Lookup lkp) {
        String title;
        DataObject dobj = lkp.lookup(DataObject.class);
        String dobjName = dobj != null ? dobj.getName() : "";
        Project prj = lkp.lookup(Project.class);
        String prjLabel = prj != null ? ProjectUtils.getInformation(prj).getDisplayName() : "No Project on Lookup";
        switch (action) {
            case ActionProvider.COMMAND_RUN:
                title = TXT_Run(prjLabel);
                break;
            case ActionProvider.COMMAND_DEBUG:
                title = TXT_Debug(prjLabel);
                break;
            case ActionProvider.COMMAND_PROFILE:
                title = TXT_Profile(prjLabel);
                break;
            case ActionProvider.COMMAND_TEST:
                title = TXT_Test(prjLabel);
                break;
            case ActionProvider.COMMAND_RUN_SINGLE:
                title = TXT_Run(dobjName);
                break;
            case ActionProvider.COMMAND_DEBUG_SINGLE:
            case ActionProvider.COMMAND_DEBUG_TEST_SINGLE:
                title = TXT_Debug(dobjName);
                break;
            case ActionProvider.COMMAND_PROFILE_SINGLE:
            case ActionProvider.COMMAND_PROFILE_TEST_SINGLE:
                title = TXT_Profile(dobjName);
                break;
            case ActionProvider.COMMAND_TEST_SINGLE:
                title = TXT_Test(dobjName);
                break;
            case "debug.fix":
                title = TXT_ApplyCodeChanges(prjLabel);
                break;
            default:
                title = TXT_Build(prjLabel);
        }
        return title;
    }

 /**
     * Returns the active platform used by the project or null if the active
     * project platform is broken.
     * @param activePlatformId the name of platform used by Ant script or null
     * for default platform.
     * @return active {@link JavaPlatform} or null if the project's platform
     * is broken
     */
    public static JavaPlatform getActivePlatform(final String activePlatformId) {
        final JavaPlatformManager pm = JavaPlatformManager.getDefault();
        if (activePlatformId == null) {
            return pm.getDefaultPlatform();
        } else {
            JavaPlatform[] installedPlatforms = pm.getPlatforms(null, new Specification("j2se", null)); //NOI18N
            for (JavaPlatform installedPlatform : installedPlatforms) {
                String antName = installedPlatform.getProperties().get("platform.ant.name"); //NOI18N
                if (antName != null && antName.equals(activePlatformId)) {
                    return installedPlatform;
                }
            }
            return null;
        }
    }

    public static JavaPlatform getActivePlatform(Project project) {
        GradleBaseProject gbp = GradleBaseProject.get(project);
        if (gbp != null) {
            String platformId = gbp.getNetBeansProperty(PROP_JDK_PLATFORM);
            if (platformId == null) {
                Preferences prefs = NbGradleProject.getPreferences(project, false);
                platformId = prefs.get(PROP_JDK_PLATFORM, "");
            }
            JavaPlatform ret = getActivePlatform(platformId);
            if (ret != null) {
                return ret;
            }
        }
        return JavaPlatformManager.getDefault().getDefaultPlatform();
    }
    private static String stringsInCurly(List<String> l) {
        StringBuilder sb = new StringBuilder("(");
        Iterator<String> it = l.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(it.hasNext() ? ", " : ")");
        }
        return sb.toString();
    }

}