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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import org.openide.filesystems.FileObject;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

/**
 *
 * @author lkishalmi
 */
public final class HelmChart {

    final String apiVersion;
    final String name;
    final String version;
    final Optional<String> description;

    public HelmChart(FileObject chartYaml) throws InvalidChartException {
        Load load = new Load(LoadSettings.builder().build());
        Map<String, Object> chart;
        try (InputStream is = chartYaml.getInputStream()) {
            Object yaml = load.loadFromInputStream(is);
            if (yaml instanceof Map) {
                chart = (Map<String, Object>) yaml;


            } else {
                throw new InvalidChartException("Invalid Chart.yaml");
            }

        } catch(IOException ex) {
            throw new InvalidChartException("Problem reading the Chart.yaml file", ex);
        }

        apiVersion = readRequiedString(chart, "apiVersion");
        name = readRequiedString(chart, "name");
        version = readRequiedString(chart, "version");

        description = readOptionalString(chart, "description");
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Optional<String> getDescription() {
        return description;
    }

    private static String readRequiedString(Map<String, Object> yaml, String key) throws InvalidChartException {
        Object value = yaml.get(key);
        if (value == null) {
            throw new InvalidChartException(key + " is required in Chart.yaml");
        } else if (!(value instanceof String)){
            throw new InvalidChartException(key + " must be a string");
        } else {
            return (String) value;
        }
    }

    private static Optional<String> readOptionalString(Map<String, Object> yaml, String key) throws InvalidChartException {
        Object value = yaml.get(key);
        if (value == null) {
            return Optional.empty();
        } else if (!(value instanceof String)){
            throw new InvalidChartException(key + " must be a string");
        } else {
            return Optional.of((String) value);
        }
    }

}
