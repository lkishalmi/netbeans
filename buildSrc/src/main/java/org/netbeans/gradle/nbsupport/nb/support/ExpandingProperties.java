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

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author lkishalmi
 */
final class ExpandingProperties extends Properties {

    private static final Pattern REPL = Pattern.compile("\\$\\{([a-zA-Z_0-9.]+)\\}");

    @Override
    public String getProperty(String key) {
        StringBuilder sb = new StringBuilder(super.getProperty(key));
        for (Matcher m; (m = REPL.matcher(sb)).find();) {
            sb.replace(m.start(), m.end(), super.getProperty(m.group(1)));
        }
        return sb.toString();
    }

}
