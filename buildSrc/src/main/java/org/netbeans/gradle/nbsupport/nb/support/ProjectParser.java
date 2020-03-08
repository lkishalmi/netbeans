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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.netbeans.gradle.nbsupport.nb.support.NbModule.Dependency;

/**
 *
 * @author lkishalmi
 */
public class ProjectParser {

    public NbModule parseProjectXML(File f, NbCluster cluster) {
        NbModule ret = null;
        XMLInputFactory factory = XMLInputFactory.newFactory();
        try (InputStream is = new FileInputStream(f)) {
            XMLEventReader events = factory.createXMLEventReader(is);
            ret = new NbModule(cluster);
            while (events.hasNext()) {
                XMLEvent tag = events.nextEvent();
                if (tag.isStartElement() && "data".equals(tag.asStartElement().getName().getLocalPart())) {
                    processData(events, ret);
                }
            }
        } catch (IOException|XMLStreamException ex) {
            ex.printStackTrace();
        }
        return ret;
    }

    private void processData(XMLEventReader events, NbModule module) throws XMLStreamException {
        Map<String, String> cpExtension = new LinkedHashMap<>();
        List<String> publicPackages = new LinkedList<>();
        List<String> friendPackages = new LinkedList<>();
        List<String> friendModules = new LinkedList<>();
        while(events.hasNext()) {
            XMLEvent evt = events.nextEvent();
            if (evt.isStartElement()) {
                StartElement element = evt.asStartElement();
                String tag = element.getName().getLocalPart();
                switch (tag) {
                    case "code-name-base":
                        module.codeNameBase = events.getElementText();
                        break;
                    case "module-dependencies":
                        module.directMainDependencies = processDependencies(events, "module-dependencies", "dependency");
                        break;
                    case "test-dependencies":
                        processTestDependencies(events, module);
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
                        cpExtension.put(relPath, origin);
                        break;
                    case "public-packages":
                        while (events.hasNext()) {
                            XMLEvent nevt = events.nextEvent();
                            if (nevt.isStartElement()) {
                                String ntag = nevt.asStartElement().getName().getLocalPart();
                                if ("package".equals(ntag)) {
                                    publicPackages.add(events.getElementText());
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
                                        friendPackages.add(events.getElementText());
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
        if (!cpExtension.isEmpty()) {
            module.classPathExtensions = cpExtension;
        }
        if (!publicPackages.isEmpty()) {
            module.publicPackages = publicPackages;
        }
        if (!friendPackages.isEmpty()) {
            module.friendPackages = friendPackages;
        }
        if (!friendModules.isEmpty()) {
            module.friendModules = friendModules;
        }
    }

    private void processTestDependencies(XMLEventReader events, NbModule module) throws XMLStreamException {
        while(events.hasNext()) {
            XMLEvent evt = events.nextEvent();
            if (evt.isStartElement()) {
                StartElement element = evt.asStartElement();
                String tag = element.getName().getLocalPart();
                if (tag.equals("name")) {
                    String testType = events.getElementText();
                    Set<Dependency> deps = processDependencies(events, "test-type", "test-dependency");
                    module.directTestDependencies.put(testType, deps);
                }
            }
            if (evt.isEndElement() && "test-dependencies".equals(evt.asEndElement().getName().getLocalPart())) break;
        }
    }

    private Set<Dependency> processDependencies(XMLEventReader events, String endTag, String dependencyTag) throws XMLStreamException {
        Set<Dependency> ret = new LinkedHashSet<>();
        Dependency dep = null;
        while(events.hasNext()) {
            XMLEvent evt = events.nextEvent();
            if (evt.isStartElement()) {
                StartElement startElement = evt.asStartElement();
                String tag = startElement.getName().getLocalPart();
                if (tag.equals(dependencyTag)) {
                    dep = new NbModule.Dependency();
                } else {
                    switch (tag) {
                        case "code-name-base":
                            dep.codeNameBase = events.getElementText();
                            break;
                        case "build-prerequisite":
                            dep.buildRequisite = true;
                            break;
                        case "compile-dependency":
                            dep.compileDependency = true;
                            break;
                        case "recursive":
                            dep.recursive = true;
                            break;
                        case "test":
                            dep.test = true;
                            break;
                        case "implementation-version":
                            dep.implementationVersion = true;
                            break;
                        case "release-version":
                            dep.releaseVersion = events.getElementText();
                            break;
                        case "specification-version":
                            dep.specificationVersion = events.getElementText();
                            break;
                        default:
                    }
                }
            }
            if (evt.isEndElement()) {
                EndElement endElement = evt.asEndElement();
                String tag = endElement.getName().getLocalPart();
                if (tag.equals(dependencyTag)) {
                    ret.add(dep);
                } else if (tag.equals(endTag)) break;
            }
        }
        return ret;
    }
}
