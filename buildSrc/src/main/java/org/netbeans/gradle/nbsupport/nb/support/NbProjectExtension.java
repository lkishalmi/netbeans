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
import java.io.PrintStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.gradle.api.Project;

/**
 *
 * @author lkishalmi
 */
public final class NbProjectExtension implements ModuleFinder {

    public static final String MODULE_JAR_DIR = "module.jar.dir";
    public static final String MODULE_JAR_NAME = "module.jar.basename";
    public static final String TEST_INCLUDES = "test.config.stableBTD.includes";
    public static final String TEST_EXCLUDES = "test.config.stableBTD.excludes";

    boolean testOnly;
    final File mainProjectDir;
    final File clusterBuildDir;
    final File testDistBaseDir;
    final String cluster;
    Properties cachedProperties;
    Properties cachedBundle;
    Manifest cachedManifest;
    NbModule module;
    final Project project;

    public NbProjectExtension(Project project) {
        this.project = project;
        File projectDir = project.getProjectDir();
        String projectDirName = projectDir.getName();
        testOnly = projectDirName.endsWith("-test");
        mainProjectDir = !testOnly ? projectDir : new File(projectDir.getParentFile(), projectDirName.substring(0, projectDirName.length()-5));
        cluster = projectDir.getParentFile().getName();
        clusterBuildDir = new File(project.getRootDir(), "build/netbeans/" + cluster);
        testDistBaseDir = new File(project.getRootDir(), "build/testdist");
    }

    public File getMainProjectDir() {
        return mainProjectDir;
    }

    public boolean isTestOnly() {
        return testOnly;
    }

    public boolean isAutoLoad() {
        return Boolean.parseBoolean(getProperty("is.autoload"));
    }

    public boolean isEager() {
        return Boolean.parseBoolean(getProperty("is.eager"));
    }

    public boolean isOsgiMode() {
        Manifest mf = getManifest();
        Attributes attrs = mf.getMainAttributes();
        return attrs.getValue("OpenIDE-Module") == null && attrs.getValue("Bundle-SymbolicName") != null;
    }

    public NbModule getModule() {
        if (module == null) {
            File projectXml = new File(mainProjectDir, "nbproject/project.xml");
            module = parseProjectXML(projectXml);
        }
        return module;
    }

    public Properties getProperties() {
        if (cachedProperties == null) {
            Properties ret = new Properties();
            ret.setProperty(MODULE_JAR_DIR, "modules");
            ret.setProperty(TEST_INCLUDES, "**/*Test.class");
            ret.setProperty("javac.source", "1.8");

            File propsFile = new File(mainProjectDir, "nbproject/project.properties");
            if (propsFile.isFile()) {

                try (InputStream is = new FileInputStream(propsFile)) {
                    ret.load(is);
                } catch (IOException ex) {

                }
            }
            cachedProperties = ret;
        }
        return cachedProperties;
    }

    public String getProperty(String key) {
        return getProperties().getProperty(key);
    }

    public Properties getBundle() {
        if (cachedBundle == null) {
            Properties ret = new Properties();
            Manifest mf = getManifest();
            Attributes attrs = mf.getMainAttributes();
            String localizingBundle = attrs.getValue("OpenIDE-Module-Localizing-Bundle");
            if (localizingBundle != null) {
                try (InputStream is = new FileInputStream(new File(mainProjectDir, "src/" + localizingBundle))) {
                    ret.load(is);
                } catch (IOException ex ) {

                }
            }
            cachedBundle = ret;
        }
        return cachedBundle;
    }

    public Manifest getManifest() {
        if (cachedManifest == null) {
            Manifest ret = new Manifest();
            try (InputStream is = new FileInputStream(new File(mainProjectDir, "manifest.mf"))) {
                ret.read(is);
            } catch (IOException ex) {
            }
            cachedManifest = ret;
        }
        return cachedManifest;
    }

    public String getImplementationVersion() {
        Manifest mf = getManifest();
        Attributes attrs = mf.getMainAttributes();
        return attrs.getValue("OpenIDE-Module-Implementation-Version");
    }

    public String getCluster() {
        return cluster;
    }

    public File getClusterBuildDir() {
        return clusterBuildDir;
    }

    public File getModuleDestDir() {
        return new File(clusterBuildDir, getProperty( MODULE_JAR_DIR));
    }

    public File getTestDestBaseDir(String type) {
        return new File(testDistBaseDir, type + '/' + cluster);
    }

    public Map<String, String> getTestProperties(String type) {
        String prefix = "test-" + type + "-sys-prop.";
        Map<String, String> ret = new HashMap<>();
        for (String key : getProperties().stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                ret.put(key.substring(prefix.length()), getProperty(key));
            }
        }
        return ret.isEmpty() ? Collections.emptyMap() : ret;
    }
    
    public String getDisplayName() {
        String moduleName = getBundle().getProperty("OpenIDE-Module-Name");
        return testOnly ? moduleName + " Test" : moduleName;
    }

    @Override
    public NbModule findOrLoadModule(String codeNameBase) {
        Project root = project.getRootProject();
        Project prj = root.getExtensions().findByType(NbClusterContainer.class).getProjectByCodeName(codeNameBase);
        if (prj != null) {
            NbProjectExtension ext = prj.getExtensions().findByType(NbProjectExtension.class);
            if (ext == null) {
                System.out.println("No extension for " + prj.getPath());
            }
            return ext.getModule();
        }
        throw new IllegalArgumentException("No project dependency ':" + codeNameBase + "' found for :" + project.getName());
    }

    public void inspectDependencies(PrintStream out) throws IOException {
        for (NbModule.DependencyType type : NbModule.DependencyType.values()) {
            inspectDependencies(0, type, out);
            out.println();
        }
    }

    public void inspectDependencies(int level, NbModule.DependencyType type, PrintStream out) throws IOException {
        NbModule module = getModule();
        Set<? extends NbModule.Dependency> directDeps = module.dependencies.get(type);
        if (level == 0) {
            out.println(module.codeNameBase + " " + type + " dependencies:");
        }
        for (NbModule.Dependency dep: directDeps) {
            for (int i = 0; i < level + 1; i++) {
                out.print("  ");
            }
            out.print("- " + dep.codeNameBase);
            if (dep.releaseVersion.isPresent()) out.print("/" + dep.releaseVersion.get());
            if (dep.implementationVersion || dep.specificationVersion.isPresent()) {
                out.print(dep.implementationVersion ? " = " : " > ");
                out.print(dep.implementationVersion ? "<impl>" : dep.specificationVersion.get());
            }
            if (dep.test) out.print(" (t)");
            if (dep.recursive) out.print(" (r)");
            out.println();
            if (dep.recursive) {
                NbModule m = findOrLoadModule(dep.codeNameBase);
                if (m != null) {
                    inspectDependencies(level + 1, dep.test? NbModule.DependencyType.TEST_UNIT : NbModule.DependencyType.MAIN, out);
                } else {
                    throw new IllegalStateException("No module '" + dep.codeNameBase+ "' as a depencency of: " + module.codeNameBase);
                }
            }
        }
    }


    NbModule parseProjectXML(File f) {
        NbModule ret = null;
        XMLInputFactory factory = XMLInputFactory.newFactory();
        try (InputStream is = new FileInputStream(f)) {
            XMLEventReader events = factory.createXMLEventReader(is);
            while (events.hasNext()) {
                XMLEvent tag = events.nextEvent();
                if (tag.isStartElement() && "data".equals(tag.asStartElement().getName().getLocalPart())) {
                    ret = processData(events);
                }
            }
        } catch (IOException|XMLStreamException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    private static NbModule processData(XMLEventReader events) throws XMLStreamException {
        Set<NbModule.ClasspathExtension> cpExtension = new LinkedHashSet<>();
        List<String> publicPackages = new LinkedList<>();
        List<String> friendPackages = new LinkedList<>();
        Set<String> friendModules = new HashSet<>();
        Map<NbModule.DependencyType, Set<NbModule.Dependency>> dependencies = new EnumMap<>(NbModule.DependencyType.class);
        String codeNameBase = null;
        while(events.hasNext()) {
            XMLEvent evt = events.nextEvent();
            if (evt.isStartElement()) {
                StartElement element = evt.asStartElement();
                String tag = element.getName().getLocalPart();
                switch (tag) {
                    case "code-name-base":
                        codeNameBase = events.getElementText();
                        break;
                    case "module-dependencies":
                        Set<NbModule.Dependency> main = new LinkedHashSet<>();
                        processDependencies(events, main, "module-dependencies", "dependency");
                        dependencies.put(NbModule.DependencyType.MAIN, main);
                        break;
                    case "test-dependencies":
                        Set<NbModule.Dependency> unit = new LinkedHashSet<>();
                        Set<NbModule.Dependency> qa = new LinkedHashSet<>();
                        processTestDependencies(events, unit, qa);
                        dependencies.put(NbModule.DependencyType.TEST_UNIT, unit);
                        dependencies.put(NbModule.DependencyType.TEST_FUNCTIONAL, qa);
                        break;
                    case "class-path-extension":
                        String relPath = null;
                        String origin = null;
                        while (events.hasNext()) {
                            XMLEvent nevt = events.nextEvent();
                            if (nevt.isStartElement()) {
                                String ntag = nevt.asStartElement().getName().getLocalPart();
                                switch (ntag) {
                                    case "runtime-relative-path":
                                        relPath = events.getElementText();
                                        break;
                                    case "binary-origin":
                                        origin = events.getElementText();
                                        break;
                                }
                            }
                            if (nevt.isEndElement() && nevt.asEndElement().getName().equals(element.getName())) break;
                        }
                        cpExtension.add(new NbModule.ClasspathExtension(relPath, origin));
                        break;
                    case "public-packages":
                        while (events.hasNext()) {
                            XMLEvent nevt = events.nextEvent();
                            if (nevt.isStartElement()) {
                                String ntag = nevt.asStartElement().getName().getLocalPart();
                                if ("package".equals(ntag)) {
                                    publicPackages.add(events.getElementText().replace('.', '/') + "/*");
                                }
                                if ("subpackages".equals(ntag)) {
                                    publicPackages.add(events.getElementText().replace('.', '/') + "/**");
                                }
                            }
                            if (nevt.isEndElement() && nevt.asEndElement().getName().equals(element.getName())) break;
                        }
                        break;
                    case "friend-packages":
                        while (events.hasNext()) {
                            XMLEvent nevt = events.nextEvent();
                            if (nevt.isStartElement()) {
                                String ntag = nevt.asStartElement().getName().getLocalPart();
                                switch (ntag) {
                                    case "friend":
                                        friendModules.add(events.getElementText());
                                        break;
                                    case "package":
                                        friendPackages.add(events.getElementText().replace('.', '/') + "/*");
                                        break;
                                }
                            }
                            if (nevt.isEndElement() && nevt.asEndElement().getName().equals(element.getName())) break;
                        }
                        break;
                }
            }
            if (evt.isEndElement() && "data".equals(evt.asEndElement().getName().getLocalPart())) break;
        }
        return new NbModule(codeNameBase, cpExtension, publicPackages, friendPackages, friendModules, dependencies);
    }

    private static void processTestDependencies(XMLEventReader events, Set<NbModule.Dependency> unitDeps, Set<NbModule.Dependency> qaDeps) throws XMLStreamException {
        while(events.hasNext()) {
            XMLEvent evt = events.nextEvent();
            if (evt.isStartElement()) {
                StartElement element = evt.asStartElement();
                String tag = element.getName().getLocalPart();
                if (tag.equals("name")) {
                    String testType = events.getElementText();
                    if ("unit".equals(testType)) {
                        processDependencies(events, unitDeps, "test-type", "test-dependency");
                    }
                    if ("qa-functional".equals(testType)){
                        processDependencies(events, qaDeps, "test-type", "test-dependency");
                    }
                }
            }
            if (evt.isEndElement() && "test-dependencies".equals(evt.asEndElement().getName().getLocalPart())) break;
        }
    }

    private static void processDependencies(XMLEventReader events, Set<NbModule.Dependency> dependencies, String endTag, String dependencyTag) throws XMLStreamException {
        while(events.hasNext()) {
            XMLEvent evt = events.nextEvent();
            if (evt.isStartElement()) {
                StartElement startElement = evt.asStartElement();
                String tag = startElement.getName().getLocalPart();
                if (tag.equals(dependencyTag)) {
                    dependencies.add(processDependency(events, dependencyTag));
                }
            }
            if (evt.isEndElement()) {
                EndElement endElement = evt.asEndElement();
                String tag = endElement.getName().getLocalPart();
                if (tag.equals(endTag)) break;
            }
        }
    }

    private static NbModule.Dependency processDependency(XMLEventReader events, String dependencyTag) throws XMLStreamException {
        String codeNameBase = null;
        boolean buildRequisite = false;
        boolean runtime = false;
        boolean compileDependency = false;
        boolean recursive = false;
        boolean test = false;
        boolean implementationVersion = true;
        Optional<String> releaseVersion = Optional.empty();
        Optional<String> specificationVersion = Optional.empty();

        while(events.hasNext()) {
            XMLEvent evt = events.nextEvent();
            if (evt.isStartElement()) {
                StartElement startElement = evt.asStartElement();
                String tag = startElement.getName().getLocalPart();
                switch (tag) {
                    case "code-name-base":
                        codeNameBase = events.getElementText();
                        break;
                    case "build-prerequisite":
                        buildRequisite = true;
                        break;
                    case "compile-dependency":
                        compileDependency = true;
                        break;
                    case "run-dependency":
                        runtime = true;
                        break;
                    case "recursive":
                        recursive = true;
                        break;
                    case "test":
                        test = true;
                        break;
                    case "implementation-version":
                        implementationVersion = true;
                        break;
                    case "release-version":
                        implementationVersion = false;
                        releaseVersion = Optional.of(events.getElementText());
                        break;
                    case "specification-version":
                        implementationVersion = false;
                        specificationVersion = Optional.of(events.getElementText());
                        break;
                    default:
                }
            }
            if (evt.isEndElement()) {
                EndElement endElement = evt.asEndElement();
                String tag = endElement.getName().getLocalPart();
                if (tag.equals(dependencyTag)) {
                    break;
                }
            }
        }
        return new NbModule.Dependency(codeNameBase, buildRequisite, runtime, compileDependency, recursive, test, implementationVersion, releaseVersion, specificationVersion);
    }
}
