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

package org.netbeans.modules.gradle.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Laszlo Kishalmi
 */
public final class GradleProject implements Serializable, Lookup.Provider {

    /**
     * As loading a Gradle project information into the memory could be a time
     * consuming task each the Gradle Plugin uses heuristics and offline 
     * evaluation of a project in order to provide optimal responsiveness.
     * E.g. If we just need to know if the project is a Gradle project, there 
     * is no need to go and fetch all the dependencies.
     * <p/>
     * Gradle project is associated with the quality of the
     * information available at the time. The quality of data can be improved,
     * by reloading the project.
     */
    public static enum Quality {

        /** The data of this project is unreliable, based on heuristics. */
        FALLBACK,
        
        /** The data of this project is unreliable. This usually means that the 
         * project was once in a better quality, but some recent change made the
         * the project un-loadable. E.g. syntax error in the recently edited 
         * buils.gradle file. The IDE cannot reload it but tries to work with 
         * the previously retrieved information. */
        EVALUATED,

        /** The data of this project is reliable, dependency information can be partial though. */
        SIMPLE,

        /** The data of this project is reliable, full dependency information is available offline. */
        FULL,

        /** The data of this project is reliable. with full dependency information. */
        FULL_ONLINE;

        public boolean betterThan(Quality q) {
            return this.ordinal() > q.ordinal();
        }

        public boolean atLeast(Quality q) {
            return this.ordinal() >= q.ordinal();
        }

        public boolean worseThan(Quality q) {
            return this.ordinal() < q.ordinal();
        }

        public boolean notBetterThan(Quality q) {
            return this.ordinal() <= q.ordinal();
        }

    }

    public static final String PRIVATE_TASK_GROUP = "<private>"; //NOI18N

    final Set<String> problems;
    final Quality quality;
    final long evaluationTime = System.currentTimeMillis();
    final Lookup lookup;
    final GradleBaseProject baseProject;

    @SuppressWarnings("rawtypes")
    public GradleProject(Quality quality, Set<String> problems, Collection infos) {
        this.quality = quality;
        Set<String> probs = new LinkedHashSet<>();
        for (String prob : problems) {
            if (prob != null) probs.add(prob);
        }
        this.problems = probs;
        InstanceContent ic = new InstanceContent();
        for (Object i : infos) {
            ic.add(i);
        }
        lookup = new AbstractLookup(ic);
        baseProject = lookup.lookup(GradleBaseProject.class);
        assert baseProject != null : "GradleProject always shall have a GradleBaseProject in it's lookup!";
    }

    private GradleProject(Quality quality, Set<String> problems, GradleProject origin) {
        this.quality = quality;
        Set<String> probs = new LinkedHashSet<>();
        for (String prob : problems) {
            if (prob != null) probs.add(prob);
        }
        this.problems = probs;
        lookup = origin.lookup;
        baseProject = lookup.lookup(GradleBaseProject.class);
        assert baseProject != null : "GradleProject always shall have a GradleBaseProject in it's lookup!";
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public Set<String> getProblems() {
        return Collections.unmodifiableSet(problems);
    }

    public Quality getQuality() {
        return quality;
    }

    public long getEvaluationTime() {
        return evaluationTime;
    }

    @NonNull
    public GradleBaseProject getBaseProject() {
        return baseProject;
    }

    @Override
    public String toString() {
        return "GradleProject{" + "quality=" + quality + ", baseProject=" + baseProject + '}';
    }

    public final GradleProject invalidate(String... reasons) {
        Set<String> p = new LinkedHashSet<>(Arrays.asList(reasons));
        return new GradleProject(Quality.EVALUATED, p, this);
    }

    public static GradleProject get(Project project) {
        NbGradleProject prj = NbGradleProject.get(project);
        return prj != null ? prj.getGradleProject() : null;
    }


}