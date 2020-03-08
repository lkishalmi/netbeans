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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    public static final String TEST_EXCLUDES = "test.config.stableBTD.excludes";

    boolean testOnly;
    final File mainProjectDir;
    final File clusterBuildDir;
    final File testDistBaseDir;
    final String cluster;
    final Properties properties = new Properties();
    final Properties bundle = new Properties();
    final Manifest manifest;
    final String implementationVersion;
    final boolean osgiMode;

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
        testDistBaseDir = new File(project.getRootDir(), "build/testdist");
        File propsFile = new File(mainProjectDir, "nbproject/project.properties");
        if (propsFile.isFile()) {
            try (InputStream is = new FileInputStream(propsFile)) {
                properties.load(is);
            } catch (IOException ex) {

            }
        }

        manifest = new Manifest();
        try (InputStream is = new FileInputStream(new File(mainProjectDir, "manifest.mf"))) {
            manifest.read(is);
        } catch (IOException ex) {
        }
        Attributes mainAttributes = manifest.getMainAttributes();

        String myself = mainAttributes.getValue("OpenIDE-Module");
        if (myself == null) {
            myself = mainAttributes.getValue("Bundle-SymbolicName");
            osgiMode = myself != null;
        } else {
            osgiMode = false;
        }

        implementationVersion = mainAttributes.getValue("OpenIDE-Module-Implementation-Version");
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

    public boolean isAutoLoad() {
        return Boolean.parseBoolean(properties.getProperty("is.autoload", "false"));
    }

    public boolean isEager() {
        return Boolean.parseBoolean(properties.getProperty("is.eager", "false"));
    }

    public boolean isOsgiMode() {
        return osgiMode;
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

    public Manifest getManifest() {
        return manifest;
    }

    public String getImplementationVersion() {
        return implementationVersion;
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

    public File getTestDestBaseDir(String type) {
        return new File(testDistBaseDir, type + '/' + cluster);
    }

    public Map<String, String> getTestProperties(String type) {
        String prefix = "test-" + type + "-sys-prop.";
        Map<String, String> ret = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                ret.put(key.substring(prefix.length()), properties.getProperty(key));
            }
        }
        return ret.isEmpty() ? Collections.emptyMap() : ret;
    }
    
    public String getDisplayName() {
        String moduleName = bundle.getProperty("OpenIDE-Module-Name");
        return testOnly ? moduleName + " Test" : moduleName;
    }
}
