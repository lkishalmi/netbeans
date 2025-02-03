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
package org.netbeans.modules.terraform.indexing;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 *
 * @author lkishalmi
 */
public class ProviderSchemas {

    public record Attribute(
            
            JsonElement type,
             String description,
             Boolean required,
             Boolean optional,
             Boolean computed,
             Boolean sensitive
            ) {}

    public record BlockType(
            @SerializedName("nesting_mode")
            String nestingMode,
             Block block,
            @SerializedName("min_items")
            Integer min,
            @SerializedName("max_items")
             Integer max
        ) {}

    public record Block(
             Map<String, Attribute> attributes,
            @SerializedName("block_types")
            Map<String, BlockType> blockTypes) {}

    public record Parameter(
             String name,
             String description,
            @SerializedName("is_nullable")
             Boolean nullable,
            JsonElement type
        ) {}
    
    public record Function(
             String summary,
             String description,
            @SerializedName("deprecation_message")
            String deprection,
            @SerializedName("return_type")
            JsonElement returnType,
             List<Parameter> parameters,
            @SerializedName("variadic_parameter")
            Parameter varArg
        ) {}
    public record Schema( long version,  Block block) {}
    public record Provider(
             Schema provider,
            @SerializedName("resource_schemas")
            Map<String, Schema> resources,
            @SerializedName("data_source_schemas")
            Map<String, Schema> dataSources,
            @SerializedName("ephemeral_resource_schemas")
            Map<String, Schema> ephemeralResources,
             Map<String, Function> functions
            ){}

    @SerializedName("format_version")
    String formatVersion;
    @SerializedName("provider_schemas")
    Map<String, Provider> providers;
    
}
