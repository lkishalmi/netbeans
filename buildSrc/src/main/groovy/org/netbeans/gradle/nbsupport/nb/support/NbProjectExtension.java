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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.gradle.api.Project;

/**
 *
 * @author lkishalmi
 */
public final class NbProjectExtension {

    public static final String MODULE_JAR_DIR = "module.jar.dir";
    public static final String MODULE_JAR_NAME = "module.jar.basename";
    public static final String TEST_INCLUDES = "test.config.stableBTD.includes";

    boolean testOnly;
    final File mainProjectDir;
    final File clusterBuildDir;
    final String cluster;
    final Properties properties = new Properties();
    final Properties bundle = new Properties();

    public NbProjectExtension(Project project) {
        properties.setProperty(MODULE_JAR_DIR, "modules");
        properties.setProperty(TEST_INCLUDES, "**/*Test.class");
        properties.setProperty("javac.source", "1.8");

        File projectDir = project.getProjectDir();
        String projectDirName = projectDir.getName();
        testOnly = projectDirName.endsWith("-test");
        mainProjectDir = !testOnly ? projectDir : new File(projectDir.getParentFile(), projectDirName.substring(0, projectDirName.length()-5));
        cluster = projectDir.getParentFile().getName();
        clusterBuildDir = new File(project.getRootDir(), "build/netbeans/" + cluster);
        File propsFile = new File(mainProjectDir, "nbproject/project.properties");
        if (propsFile.isFile()) {
            try (InputStream is = new FileInputStream(propsFile)) {
                properties.load(is);
            } catch (IOException ex) {

            }
        }
        Manifest mf = new Manifest();
        try (InputStream is = new FileInputStream(new File(mainProjectDir, "manifest.mf"))) {
            mf.read(is);
        } catch (IOException ex) {
        }
        Attributes mainAttributes = mf.getMainAttributes();
        String localizingBundle = mainAttributes.getValue("OpenIDE-Module-Localizing-Bundle");
        if (localizingBundle != null) {
            try (InputStream is = new FileInputStream(new File(mainProjectDir, "src/" + localizingBundle))) {
                bundle.load(is);
            } catch (IOException ex ) {

            }
        }
    }

    public File getMainProjectDir() {
        return mainProjectDir;
    }

    public boolean isTestOnly() {
        return testOnly;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public Properties getBundle() {
        return bundle;
    }
    
    public String getCluster() {
        return cluster;
    }

    public File getClusterBuildDir() {
        return clusterBuildDir;
    }

    public File getModuleDestDir() {
        return new File(clusterBuildDir, properties.getProperty( MODULE_JAR_DIR));
    }
    
    public String getDisplayName() {
        String moduleName = bundle.getProperty("OpenIDE-Module-Name");
        return testOnly ? moduleName + " Test" : moduleName;
    }
}
