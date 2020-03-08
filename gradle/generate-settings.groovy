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

import groovy.util.XmlSlurper;

def rootDir = new File(System.getProperty('user.dir'), '..')

def excludedModules = [
    'org.openide.util.enumerations'
]

def clusterDirs = [\
    'apisupport', \
    'enterprise', \
    'ergonomics',
    'extide',
    'groovy',
    'harness',
    'ide',
    'java',
    'javafx',
    'nb',
    'php',
    'platform',
    'profiler',
    'webcommon',
    'websvccommon'
]
File settings = new File(rootDir, 'settings.gradle');

settings.write('''/*
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

rootProject.name = 'netbeans'

def clusters = [
''');

clusterDirs.each() { cluster ->
    settings.append("    '$cluster': [\n")
    new File(rootDir, cluster).eachDir { sub ->
        File projectXML = new File (sub, 'nbproject/project.xml')
        if (projectXML.canRead()) {
            def prj = new XmlSlurper().parse(projectXML)
            if ('org.netbeans.modules.apisupport.project'.equals(prj.type.text())) {
                def name = prj.configuration.data['code-name-base'].text()
                if (!excludedModules.contains(name)) {
                    settings.append("        '$name': '${sub.name}',\n")
                    def testDir = new File(sub.getParentFile(), "${sub.getName()}-test")
                    if (testDir.isDirectory()) {
                        settings.append("        '${name}-test': '${sub.name}-test',\n")
                    }
                }
            }
        }
    }
    settings.append("    ],\n");
}
settings.append(''']

clusters.each { cluster, modules ->
    modules.each { name, dir ->\n\
        def pname = ':' + name
        include pname
        project(pname).projectDir = new File(rootDir, "${cluster}/${dir}")
    }
}
''')
