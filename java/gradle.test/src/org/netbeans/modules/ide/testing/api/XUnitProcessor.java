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
package org.netbeans.modules.ide.testing.api;

import java.io.File;
import java.io.FileInputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author lkishalmi
 */
public class XUnitProcessor {


    private static class XUnitParser {
        final TestSession session;
        
        XUnitParser(TestSession session) {
            this.session = session;
        }
        
        public void parse(File f) throws Exception {
            TestSuite ts = null;
            TestCase tc = null;
            XMLInputFactory factory = XMLInputFactory.newFactory();
            XMLEventReader events = factory.createXMLEventReader(new FileInputStream(f));
            while (events.hasNext()) {
                XMLEvent evt = events.nextTag();
                if (evt.isStartElement()) {
                    StartElement element = evt.asStartElement();
                    switch (evt.asStartElement().getName().getLocalPart()) {
                        case "testsuite": {
                            String name  = element.getAttributeByName(QName.valueOf("name")).getValue();
                            ts = session.newTestSuite(name);
                            break;
                        }
                        case "testcase": {
                            Attribute name = element.getAttributeByName(QName.valueOf("name"));
                            Attribute classname = element.getAttributeByName(QName.valueOf("classname"));
                            String tcName = name != null ? name.getValue() : classname.getValue();
                            tc = ts.newTestCase(tcName);
                            break;
                        }
                    }
                }
            }
        }

    }
}
