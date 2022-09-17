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
package org.netbeans.modules.helm;

import java.nio.file.Path;

/**
 *
 * @author lkishalmi
 */
public final class HelmFiles {
    
    public static final String CHART_NAME = "Chart.yaml"; //NOI18N
    public static final String TEMPLATES_NAME = "templates"; //NOI18N
    public static final String CHARTS_NAME = "charts"; //NOI18N
    public static final String VALUES_NAME = "values.yaml"; //NOI18N
    
    private final Path root;
    
    public HelmFiles(Path root) {
        this.root = root;
    }
    
    public Path getTemplatesDir() {
        return root.resolve(TEMPLATES_NAME);
    }
    
    public Path getChartYaml() {
        return root.resolve(CHART_NAME);
    }
    
    public Path getValuesYaml() {
        return root.resolve(VALUES_NAME);
    }
}
